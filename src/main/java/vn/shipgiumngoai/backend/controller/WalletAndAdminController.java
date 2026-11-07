package vn.shipgiumngoai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.shipgiumngoai.backend.model.entity.*;
import vn.shipgiumngoai.backend.model.enums.Role;
import vn.shipgiumngoai.backend.model.enums.TransactionType;
import vn.shipgiumngoai.backend.model.enums.VerificationStatus;
import vn.shipgiumngoai.backend.repository.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WalletAndAdminController {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverBlacklistRepository driverBlacklistRepository;
    private final BlacklistAttemptLogRepository blacklistAttemptLogRepository;
    private final VendorRepository vendorRepository;
    private final EscrowTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final DriverReviewReportRepository reviewRepository;
    private final SystemShiftConfigRepository shiftConfigRepository;

    public WalletAndAdminController(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverBlacklistRepository driverBlacklistRepository,
            BlacklistAttemptLogRepository blacklistAttemptLogRepository,
            VendorRepository vendorRepository,
            EscrowTransactionRepository transactionRepository,
            WithdrawalRequestRepository withdrawalRepository,
            DriverReviewReportRepository reviewRepository,
            SystemShiftConfigRepository shiftConfigRepository
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverBlacklistRepository = driverBlacklistRepository;
        this.blacklistAttemptLogRepository = blacklistAttemptLogRepository;
        this.vendorRepository = vendorRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.reviewRepository = reviewRepository;
        this.shiftConfigRepository = shiftConfigRepository;
    }

    public record WithdrawInput(Long amount, String bankName, String bankAccountNumber, String accountHolderName) {
    }

    public record ShiftUpdateInput(Boolean shiftOpen, String todayCustomHours, String statusBannerMessage) {
    }

    // 1. KHÁCH HÀNG HOẶC TÀI XẾ YÊU CẦU RÚT TIỀN TỪ VÍ NỘI BỘ VỀ TÀI KHOẢN NGÂN HÀNG (CHỜ ADMIN DUYỆT)
    @PostMapping("/wallets/withdraw")
    @Transactional
    public ResponseEntity<?> requestWithdrawal(
            @AuthenticationPrincipal User currentUser,
            @RequestBody WithdrawInput req
    ) {
        if (req.amount() == null || req.amount() < 10000L) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số tiền rút tối thiểu là 10.000đ"));
        }

        User user = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String walletType = (user.getRole() == Role.DRIVER) ? "DRIVER_WALLET" : "CUSTOMER_WALLET";
        long currentBalance = (user.getRole() == Role.DRIVER)
                ? user.getDriverWalletBalance()
                : user.getCustomerWalletBalance();

        if (currentBalance < req.amount()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số dư ví hiện tại không đủ để rút!"));
        }

        if (user.getRole() == Role.DRIVER) {
            user.setDriverWalletBalance(currentBalance - req.amount());
        } else {
            user.setCustomerWalletBalance(currentBalance - req.amount());
        }
        userRepository.save(user);

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setUser(user);
        wr.setWalletType(walletType);
        wr.setAmount(req.amount());
        wr.setBankName(req.bankName() != null ? req.bankName() : "MB Bank");
        wr.setBankAccountNumber(req.bankAccountNumber() != null ? req.bankAccountNumber() : "0123456789");
        wr.setAccountHolderName(req.accountHolderName() != null ? req.accountHolderName() : user.getFullName().toUpperCase());
        wr.setStatus("PENDING");
        wr = withdrawalRepository.save(wr);

        transactionRepository.save(new EscrowTransaction(
                null,
                user.getId(),
                TransactionType.WITHDRAWAL,
                req.amount(),
                "Yêu cầu rút " + req.amount() + "đ về ngân hàng " + wr.getBankName() + " (" + wr.getBankAccountNumber() + ")"
        ));

        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi yêu cầu rút tiền về Server! Admin sẽ đối soát và chuyển khoản cho bạn.",
                "withdrawal", wr,
                "customerWalletBalance", user.getCustomerWalletBalance(),
                "driverWalletBalance", user.getDriverWalletBalance()
        ));
    }

    @GetMapping("/wallets/my-transactions")
    public ResponseEntity<?> getMyTransactions(@AuthenticationPrincipal User user) {
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        return ResponseEntity.ok(Map.of(
                "customerWalletBalance", freshUser.getCustomerWalletBalance(),
                "driverWalletBalance", freshUser.getDriverWalletBalance(),
                "transactions", transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "withdrawals", withdrawalRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        ));
    }

    // 2. CẬP NHẬT SỐ TIỀN MẶT ĐANG CẦM TRONG TÚI CỦA TÀI XẾ & TRẠNG THÁI ONLINE
    @PostMapping("/driver/profile/update-cash")
    @Transactional
    public ResponseEntity<?> updateDriverCashAndOnline(
            @AuthenticationPrincipal User driverUser,
            @RequestBody Map<String, Object> body
    ) {
        DriverProfile profile = driverProfileRepository.findByUserId(driverUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ tài xế"));

        if (body.get("cashOnHand") instanceof Number num) {
            profile.setCashOnHand(num.longValue());
        }
        if (body.get("online") instanceof Boolean online) {
            profile.setOnline(online);
        }
        profile = driverProfileRepository.save(profile);
        return ResponseEntity.ok(profile);
    }

    // 3. ADMIN DASHBOARD OVERVIEW
    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> getAdminDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shiftConfig", shiftConfigRepository.findById(1L).orElseGet(SystemShiftConfig::new));
        data.put("driverProfiles", driverProfileRepository.findAll());
        data.put("blacklist", driverBlacklistRepository.findAll());
        data.put("blacklistAttemptLogs", blacklistAttemptLogRepository.findAllByOrderByAttemptedAtDesc());
        data.put("pendingVendors", vendorRepository.findByApprovalStatusOrderByIdAsc("PENDING"));
        data.put("approvedVendors", vendorRepository.findByApprovalStatusOrderByIdAsc("APPROVED"));
        data.put("withdrawals", withdrawalRepository.findAllByOrderByCreatedAtDesc());
        data.put("reviewsAndReports", reviewRepository.findAllByOrderByCreatedAtDesc());
        data.put("transactions", transactionRepository.findAllByOrderByCreatedAtDesc());
        return ResponseEntity.ok(data);
    }

    // 4. ADMIN ĐIỀU CHỈNH KHUNG GIỜ SHIP (18:00 - 23:00 LINH HOẠT) & BẬT/TẮT CA
    @PostMapping("/admin/shift-config")
    @Transactional
    public ResponseEntity<?> updateShiftConfig(@RequestBody ShiftUpdateInput req) {
        SystemShiftConfig cfg = shiftConfigRepository.findById(1L).orElseGet(SystemShiftConfig::new);
        if (req.shiftOpen() != null) {
            cfg.setShiftOpen(req.shiftOpen());
            if (req.shiftOpen()) {
                cfg.setWaitingNotificationSubscribers(0);
            }
        }
        if (req.todayCustomHours() != null && !req.todayCustomHours().isBlank()) {
            cfg.setTodayCustomHours(req.todayCustomHours().trim());
        }
        if (req.statusBannerMessage() != null && !req.statusBannerMessage().isBlank()) {
            cfg.setStatusBannerMessage(req.statusBannerMessage().trim());
        }
        cfg.setUpdatedAt(LocalDateTime.now());
        cfg = shiftConfigRepository.save(cfg);
        return ResponseEntity.ok(cfg);
    }

    // 5. ADMIN DUYỆT HỒ SƠ SHIPPER SAU KHI HẸN PHỎNG VẤN TRỰC TIẾP
    @PostMapping("/admin/drivers/{profileId}/verify")
    @Transactional
    public ResponseEntity<?> verifyDriverAfterInterview(
            @PathVariable Long profileId,
            @RequestBody Map<String, String> body
    ) {
        DriverProfile profile = driverProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ tài xế"));

        String statusStr = body.getOrDefault("status", "APPROVED");
        profile.setVerificationStatus(VerificationStatus.valueOf(statusStr));
        profile.setInterviewNotes(body.getOrDefault("interviewNotes", "Đã phỏng vấn trực tiếp - Đạt yêu cầu"));
        profile = driverProfileRepository.save(profile);
        return ResponseEntity.ok(profile);
    }

    // 6. ADMIN PHẠT GẬY SHIPPER (ĐỦ 3 GẬY -> KHÓA VĨNH VIỄN + ĐẨY VÀO BLACKLIST CCCD/SĐT)
    @PostMapping("/admin/drivers/{profileId}/strike")
    @Transactional
    public ResponseEntity<?> issueDriverStrike(
            @PathVariable Long profileId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        DriverProfile profile = driverProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ tài xế"));

        int newStrikes = profile.getStrikeCount() + 1;
        profile.setStrikeCount(newStrikes);
        String reason = body != null && body.get("reason") != null
                ? body.get("reason")
                : "Vi phạm quy tắc ứng xử / Khách hàng báo cáo nói xấu Ngoại";

        User driverUser = profile.getUser();
        if (newStrikes >= 3) {
            profile.setVerificationStatus(VerificationStatus.BLACKLISTED);
            profile.setOnline(false);
            driverUser.setBanned(true);
            userRepository.save(driverUser);

            if (!driverBlacklistRepository.existsByCccdNumber(profile.getCccdNumber())) {
                driverBlacklistRepository.save(new DriverBlacklist(
                        profile.getCccdNumber(),
                        driverUser.getPhone(),
                        driverUser.getFullName(),
                        "Đủ 3 gậy vi phạm (" + reason + ")"
                ));
            }
        }
        profile = driverProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of(
                "message", newStrikes >= 3
                        ? "Đã phạt gậy thứ 3! Tài xế đã bị khóa vĩnh viễn và đưa số CCCD vào Blacklist!"
                        : "Đã phạt gậy (" + newStrikes + "/3) đối với tài xế " + driverUser.getFullName(),
                "driverProfile", profile
        ));
    }

    // 7. ADMIN DUYỆT THÔNG TIN CỦA NGOẠI DO CỘNG ĐỒNG ĐÓNG GÓP & RESET TRẠNG THÁI NGHỈ HÔM NAY
    @PostMapping("/admin/vendors/{vendorId}/approve")
    @Transactional
    public ResponseEntity<?> approveVendor(
            @PathVariable Long vendorId,
            @RequestBody Map<String, Object> body
    ) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ Ngoại"));

        if (body.get("approvalStatus") != null) {
            vendor.setApprovalStatus(body.get("approvalStatus").toString());
        }
        if (body.get("closedToday") instanceof Boolean closed) {
            vendor.setClosedToday(closed);
        }
        vendor = vendorRepository.save(vendor);
        return ResponseEntity.ok(vendor);
    }

    // 8. ADMIN DUYỆT YÊU CẦU RÚT TIỀN VỀ NGÂN HÀNG CHO KHÁCH / TÀI XẾ
    @PostMapping("/admin/withdrawals/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveWithdrawal(@PathVariable Long id) {
        WithdrawalRequest wr = withdrawalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu rút tiền"));
        wr.setStatus("APPROVED_TRANSFERRED");
        wr.setProcessedAt(LocalDateTime.now());
        wr = withdrawalRepository.save(wr);
        return ResponseEntity.ok(wr);
    }
}
