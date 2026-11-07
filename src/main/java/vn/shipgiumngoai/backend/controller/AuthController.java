package vn.shipgiumngoai.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.shipgiumngoai.backend.model.entity.BlacklistAttemptLog;
import vn.shipgiumngoai.backend.model.entity.DriverBlacklist;
import vn.shipgiumngoai.backend.model.entity.DriverProfile;
import vn.shipgiumngoai.backend.model.entity.User;
import vn.shipgiumngoai.backend.model.enums.Role;
import vn.shipgiumngoai.backend.model.enums.VerificationStatus;
import vn.shipgiumngoai.backend.repository.BlacklistAttemptLogRepository;
import vn.shipgiumngoai.backend.repository.DriverBlacklistRepository;
import vn.shipgiumngoai.backend.repository.DriverProfileRepository;
import vn.shipgiumngoai.backend.repository.UserRepository;
import vn.shipgiumngoai.backend.security.JwtService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverBlacklistRepository driverBlacklistRepository;
    private final BlacklistAttemptLogRepository blacklistAttemptLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    public AuthController(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverBlacklistRepository driverBlacklistRepository,
            BlacklistAttemptLogRepository blacklistAttemptLogRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverBlacklistRepository = driverBlacklistRepository;
        this.blacklistAttemptLogRepository = blacklistAttemptLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.messagingTemplate = messagingTemplate;
    }

    public record RegisterCustomerRequest(String phone, String fullName, String password) {
    }

    public record RegisterDriverRequest(
            String phone,
            String fullName,
            String password,
            String cccdNumber,
            String cccdFrontPath,
            String cccdBackPath,
            String vehiclePlate,
            Long cashOnHand,
            Boolean agreedCashTerms
    ) {
    }

    public record LoginRequest(String phone, String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerRequest req) {
        if (req.phone() == null || req.phone().isBlank() || req.password() == null || req.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập đầy đủ SĐT và mật khẩu"));
        }
        if (userRepository.existsByPhone(req.phone().trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã được đăng ký"));
        }

        User user = new User(
                req.phone().trim(),
                req.fullName() != null && !req.fullName().isBlank() ? req.fullName().trim() : "Khách thương Ngoại",
                passwordEncoder.encode(req.password()),
                Role.CUSTOMER
        );
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(buildAuthResponse(user, token, null));
    }

    @PostMapping("/register-driver")
    public ResponseEntity<?> registerDriver(@RequestBody RegisterDriverRequest req, HttpServletRequest httpReq) {
        if (req.phone() == null || req.cccdNumber() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng điền đầy đủ SĐT, Số CCCD và ảnh chụp CCCD 2 mặt"));
        }

        String cleanPhone = req.phone().trim();
        String cleanCccd = req.cccdNumber().trim();

        // 1. KIỂM TRA BLACKLIST TỰ ĐỘNG (AUTO-KICK + GHI LOG BÁO ADMIN ĐỂ TRÌNH CÔNG AN NẾU QUẤY RỐI)
        Optional<DriverBlacklist> blacklisted = driverBlacklistRepository.findByCccdNumberOrPhone(cleanCccd, cleanPhone);
        if (blacklisted.isPresent()) {
            String clientIp = httpReq.getRemoteAddr();
            String alertMsg = "CẢNH BÁO: Đối tượng trong Blacklist (" + blacklisted.get().getFullName()
                    + " - CCCD: " + cleanCccd + ") vừa cố tình đăng ký lại bằng SĐT: " + cleanPhone;

            BlacklistAttemptLog log = new BlacklistAttemptLog(
                    cleanCccd,
                    cleanPhone,
                    req.fullName() != null ? req.fullName() : "Không rõ",
                    clientIp,
                    req.cccdFrontPath() != null ? req.cccdFrontPath() : "cccd_front_captured.jpg",
                    alertMsg
            );
            blacklistAttemptLogRepository.save(log);

            // Bắn cảnh báo thời gian thực cho Admin
            messagingTemplate.convertAndSend("/topic/admin/alerts", Map.of(
                    "type", "BLACKLIST_INTRUSION",
                    "message", alertMsg,
                    "cccd", cleanCccd,
                    "phone", cleanPhone,
                    "ip", clientIp
            ));

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "blacklisted", true,
                    "message", "Hồ sơ CCCD/SĐT của bạn đã nằm trong danh sách cấm vĩnh viễn của Ship Giùm Ngoại do vi phạm quy tắc. Mọi thông tin truy cập đã được ghi nhận và gửi về Ban Quản Trị."
            ));
        }

        if (userRepository.existsByPhone(cleanPhone)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã tồn tại trên hệ thống"));
        }

        // 2. HỒ SƠ SẠCH -> LƯU CHỜ HẸN PHỎNG VẤN TRỰC TIẾP VỚI ADMIN
        User driverUser = new User(
                cleanPhone,
                req.fullName() != null ? req.fullName().trim() : "Tài xế mới",
                passwordEncoder.encode(req.password()),
                Role.DRIVER
        );
        driverUser = userRepository.save(driverUser);

        DriverProfile profile = new DriverProfile();
        profile.setUser(driverUser);
        profile.setCccdNumber(cleanCccd);
        profile.setCccdFrontPath(req.cccdFrontPath() != null ? req.cccdFrontPath() : "private/cccd_front.jpg");
        profile.setCccdBackPath(req.cccdBackPath() != null ? req.cccdBackPath() : "private/cccd_back.jpg");
        profile.setVehiclePlate(req.vehiclePlate() != null ? req.vehiclePlate() : "59-X1 123.45");
        profile.setCashOnHand(req.cashOnHand() != null ? req.cashOnHand() : 300000L);
        profile.setAgreedCashTerms(req.agreedCashTerms() == null || req.agreedCashTerms());
        profile.setVerificationStatus(VerificationStatus.PENDING_INTERVIEW);
        profile.setCurrentLat(10.7769);
        profile.setCurrentLng(106.7009);
        profile = driverProfileRepository.save(profile);

        String token = jwtService.generateToken(driverUser);
        return ResponseEntity.ok(buildAuthResponse(driverUser, token, profile));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.phone() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập SĐT và mật khẩu"));
        }
        Optional<User> userOpt = userRepository.findByPhone(req.phone().trim());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sai số điện thoại hoặc mật khẩu"));
        }

        User user = userOpt.get();
        if (user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Tài khoản của bạn đã bị khóa vĩnh viễn do vi phạm quy định (đủ 3 gậy)."
            ));
        }

        DriverProfile driverProfile = null;
        if (user.getRole() == Role.DRIVER) {
            driverProfile = driverProfileRepository.findByUserId(user.getId()).orElse(null);
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(buildAuthResponse(user, token, driverProfile));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        DriverProfile driverProfile = null;
        if (freshUser.getRole() == Role.DRIVER) {
            driverProfile = driverProfileRepository.findByUserId(freshUser.getId()).orElse(null);
        }
        return ResponseEntity.ok(buildAuthResponse(freshUser, null, driverProfile));
    }

    private Map<String, Object> buildAuthResponse(User user, String token, DriverProfile driverProfile) {
        Map<String, Object> res = new HashMap<>();
        if (token != null) {
            res.put("token", token);
        }
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("phone", user.getPhone());
        userMap.put("fullName", user.getFullName());
        userMap.put("role", user.getRole().name());
        userMap.put("customerWalletBalance", user.getCustomerWalletBalance());
        userMap.put("driverWalletBalance", user.getDriverWalletBalance());
        res.put("user", userMap);
        if (driverProfile != null) {
            res.put("driverProfile", driverProfile);
        }
        return res;
    }
}
