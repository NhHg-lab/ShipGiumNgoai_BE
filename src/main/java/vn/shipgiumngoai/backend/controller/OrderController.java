package vn.shipgiumngoai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.shipgiumngoai.backend.model.entity.*;
import vn.shipgiumngoai.backend.model.enums.*;
import vn.shipgiumngoai.backend.repository.*;
import vn.shipgiumngoai.backend.util.GeoUtils;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final VendorDishRepository dishRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final EscrowTransactionRepository transactionRepository;
    private final DriverReviewReportRepository reviewRepository;
    private final SystemShiftConfigRepository shiftConfigRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderController(
            OrderRepository orderRepository,
            VendorRepository vendorRepository,
            VendorDishRepository dishRepository,
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            EscrowTransactionRepository transactionRepository,
            DriverReviewReportRepository reviewRepository,
            SystemShiftConfigRepository shiftConfigRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
        this.dishRepository = dishRepository;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.transactionRepository = transactionRepository;
        this.reviewRepository = reviewRepository;
        this.shiftConfigRepository = shiftConfigRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public record OrderItemInput(Long dishId, Integer quantity) {
    }

    public record CreateOrderRequest(
            Long vendorId,
            DeliveryType deliveryType,
            PaymentMethod paymentMethod,
            String deliveryAddress,
            Double deliveryLat,
            Double deliveryLng,
            Long vendorGiftAmount,
            Long driverTipAmount,
            List<OrderItemInput> items
    ) {
    }

    public record ClosedChoiceRequest(
            String option, // OPTION_1_REFUND_100, OPTION_2_TIP_GAS, OPTION_3_SWITCH_VENDOR
            Long gasTipAmount,
            Long newVendorId,
            Long newDishId,
            Integer newQuantity
    ) {
    }

    public record ReviewDriverRequest(Integer rating, ReportTag reportTag, String comment) {
    }

    // 1.BACKEND TÍNH GIÁ 100% (KHÔNG TIN DỮ LIỆU TỪ FRONTEND)
    @PostMapping("/orders/quote")
    public ResponseEntity<?> calculateQuote(@RequestBody CreateOrderRequest req) {
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin của Ngoại"));

        long foodAmount = calculateFoodAmount(req.items());
        double lat = req.deliveryLat() != null ? req.deliveryLat() : 10.7805;
        double lng = req.deliveryLng() != null ? req.deliveryLng() : 106.6995;
        double distKm = GeoUtils.distanceKm(vendor.getLatitude(), vendor.getLongitude(), lat, lng);

        DeliveryType dType = req.deliveryType() != null ? req.deliveryType() : DeliveryType.INSTANT;
        long shippingFee = GeoUtils.calculateShippingFee(distKm, dType);
        long platformFee = 5000L;
        long vendorGift = req.vendorGiftAmount() != null && req.vendorGiftAmount() > 0 ? req.vendorGiftAmount() : 0L;
        long driverTip = req.driverTipAmount() != null && req.driverTipAmount() > 0 ? req.driverTipAmount() : 0L;
        long totalEscrow = foodAmount + vendorGift + shippingFee + platformFee + driverTip;

        return ResponseEntity.ok(Map.of(
                "distanceKm", distKm,
                "foodAmount", foodAmount,
                "vendorGiftAmount", vendorGift,
                "reimbursementAmount", foodAmount + vendorGift,
                "shippingFee", shippingFee,
                "platformFee", platformFee,
                "driverTipAmount", driverTip,
                "totalEscrowAmount", totalEscrow
        ));
    }

    // 2. TẠO ĐƠN HÀNG MỚI
    @PostMapping("/orders")
    @Transactional
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal User currentUser,
            @RequestBody CreateOrderRequest req
    ) {
        SystemShiftConfig shift = shiftConfigRepository.findById(1L).orElseGet(SystemShiftConfig::new);
        if (!shift.isShiftOpen()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Hiện tại chưa tới khung giờ mở ca ship (" + shift.getTodayCustomHours() + "). Bạn hãy bấm nút '🔔 Báo cho tôi khi mở ca' nhé!"
            ));
        }

        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin của Ngoại"));
        if (vendor.isClosedToday()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Hôm nay Ngoại nghỉ bán (đã được Shipper xác nhận). Bạn vui lòng chọn ủng hộ Ngoại khác nhé!"));
        }

        User customer = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setVendor(vendor);
        order.setOriginalVendor(vendor);
        order.setDeliveryType(req.deliveryType() != null ? req.deliveryType() : DeliveryType.INSTANT);
        order.setPaymentMethod(req.paymentMethod() != null ? req.paymentMethod() : PaymentMethod.VIETQR);
        order.setDeliveryAddress(req.deliveryAddress() != null && !req.deliveryAddress().isBlank()
                ? req.deliveryAddress() : "Đại học Bách Khoa TP.HCM, Quận 10");

        double lat = req.deliveryLat() != null ? req.deliveryLat() : 10.7726;
        double lng = req.deliveryLng() != null ? req.deliveryLng() : 106.6579;
        order.setDeliveryLat(lat);
        order.setDeliveryLng(lng);

        double distKm = GeoUtils.distanceKm(vendor.getLatitude(), vendor.getLongitude(), lat, lng);
        order.setDistanceKm(distKm);

        long foodAmount = 0L;
        for (OrderItemInput itemInput : req.items()) {
            VendorDish dish = dishRepository.findById(itemInput.dishId())
                    .orElseThrow(() -> new IllegalArgumentException("Món ăn không tồn tại"));
            int qty = itemInput.quantity() != null && itemInput.quantity() > 0 ? itemInput.quantity() : 1;
            foodAmount += dish.getEstimatedPrice() * qty;
            order.addItem(new OrderItem(dish.getId(), dish.getName(), qty, dish.getEstimatedPrice()));
        }

        long vendorGift = req.vendorGiftAmount() != null && req.vendorGiftAmount() > 0 ? req.vendorGiftAmount() : 0L;
        long driverTip = req.driverTipAmount() != null && req.driverTipAmount() > 0 ? req.driverTipAmount() : 0L;
        long shippingFee = GeoUtils.calculateShippingFee(distKm, order.getDeliveryType());
        long platformFee = 5000L;
        long totalEscrow = foodAmount + vendorGift + shippingFee + platformFee + driverTip;

        order.setFoodAmount(foodAmount);
        order.setVendorGiftAmount(vendorGift);
        order.setShippingFee(shippingFee);
        order.setPlatformFee(platformFee);
        order.setDriverTipAmount(driverTip);
        order.setTotalEscrowAmount(totalEscrow);

        // Nếu khách chọn thanh toán bằng Số dư Ví Khách Hàng (Customer Refund Wallet)
        if (order.getPaymentMethod() == PaymentMethod.CUSTOMER_WALLET) {
            if (customer.getCustomerWalletBalance() < totalEscrow) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Số dư Ví Hoàn Tiền của bạn (" + customer.getCustomerWalletBalance() + "đ) không đủ thanh toán đơn này (" + totalEscrow + "đ). Vui lòng chọn quét VietQR!"
                ));
            }
            customer.setCustomerWalletBalance(customer.getCustomerWalletBalance() - totalEscrow);
            userRepository.save(customer);

            order.setStatus(OrderStatus.PAID_FINDING_DRIVER);
            order = orderRepository.save(order);

            transactionRepository.save(new EscrowTransaction(
                    order.getId(),
                    customer.getId(),
                    TransactionType.PAY_FROM_CUSTOMER_WALLET,
                    totalEscrow,
                    "Thanh toán đơn #" + order.getId() + " từ Số dư Ví Khách Hàng sang Ví thứ 3 (Escrow)"
            ));
            broadcastOrderUpdate(order);
        } else {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order = orderRepository.save(order);
        }

        return ResponseEntity.ok(order);
    }

    // 3. XÁC NHẬN ĐÃ CHUYỂN KHOẢN VIETQR VÀO VÍ THỨ 3 (ESCROW WEBHOOK / CONFIRM)
    @PostMapping("/orders/{id}/confirm-payment")
    @Transactional
    public ResponseEntity<?> confirmVietQrPayment(@PathVariable Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAID_FINDING_DRIVER);
            order = orderRepository.save(order);

            transactionRepository.save(new EscrowTransaction(
                    order.getId(),
                    order.getCustomer().getId(),
                    TransactionType.ESCROW_HOLD,
                    order.getTotalEscrowAmount(),
                    "PayOS VietQR: Tạm giữ " + order.getTotalEscrowAmount() + "đ trong Ví thứ 3 cho đơn #" + order.getId()
            ));
            broadcastOrderUpdate(order);
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/my")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal User user) {
        if (user.getRole() == Role.DRIVER) {
            return ResponseEntity.ok(orderRepository.findByDriverIdOrderByCreatedAtDesc(user.getId()));
        }
        return ResponseEntity.ok(orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId()));
    }

    // 4. DANH SÁCH ĐƠN ĐANG TÌM SHIPPER (LỌC THEO TIỀN MẶT ĐANG CÓ VỚI ĐƠN GIAO NHANH)
    @GetMapping("/driver/orders/available")
    public ResponseEntity<?> getAvailableOrdersForDriver(@AuthenticationPrincipal User driverUser) {
        DriverProfile profile = driverProfileRepository.findByUserId(driverUser.getId()).orElse(null);
        long cashOnHand = profile != null ? profile.getCashOnHand() : 500000L;

        List<Order> findingDriverOrders = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PAID_FINDING_DRIVER);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Order o : findingDriverOrders) {
            long cashNeeded = o.getReimbursementAmount();
            boolean enoughCash = cashOnHand >= cashNeeded;
            boolean canAccept = (o.getDeliveryType() == DeliveryType.FLEXIBLE) || enoughCash;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("order", o);
            item.put("cashNeededForVendor", cashNeeded);
            item.put("driverCashOnHand", cashOnHand);
            item.put("canAccept", canAccept);
            item.put("cashWarning", !enoughCash
                    ? (o.getDeliveryType() == DeliveryType.INSTANT
                        ? "Đơn Giao Nhanh yêu cầu có sẵn " + cashNeeded + "đ tiền mặt"
                        : "Đơn Giao Linh Hoạt: Bạn có thể nhận đơn rồi ghé ATM rút " + cashNeeded + "đ tiền mặt")
                    : "Đủ tiền mặt nhận ngay");
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    // 5. TÀI XẾ NHẬN ĐƠN (KHÓA PESSIMISTIC WRITE LOCK CHỐNG 2 TÀI XẾ NHẬN TRÙNG)
    @PostMapping("/driver/orders/{id}/accept")
    @Transactional
    public ResponseEntity<?> driverAcceptOrder(
            @AuthenticationPrincipal User driverUser,
            @PathVariable Long id
    ) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PAID_FINDING_DRIVER) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng này đã có Shipper khác nhận rồi!"));
        }

        DriverProfile profile = driverProfileRepository.findByUserId(driverUser.getId()).orElse(null);
        if (profile != null) {
            if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
                return ResponseEntity.badRequest().body(Map.of("message", "Hồ sơ Shipper của bạn đang chờ phỏng vấn/duyệt từ Admin!"));
            }
            if (order.getDeliveryType() == DeliveryType.INSTANT && profile.getCashOnHand() < order.getReimbursementAmount()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Đơn Giao Nhanh bắt buộc có sẵn ít nhất " + order.getReimbursementAmount() + "đ tiền mặt trong người!"
                ));
            }
        }

        order.setDriver(driverUser);
        order.setStatus(OrderStatus.DRIVER_ASSIGNED);
        order = orderRepository.save(order);

        broadcastOrderUpdate(order);
        return ResponseEntity.ok(order);
    }

    // 6. TÀI XẾ MUA HÀNG TRẢ TIỀN MẶT CHO NGOẠI & CHỤP ẢNH BẰNG CHỨNG TẠI GÁNH HÀNG
    @PostMapping("/driver/orders/{id}/confirm-purchase")
    @Transactional
    public ResponseEntity<?> confirmPurchasedAtVendor(
            @AuthenticationPrincipal User driverUser,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        String proofUrl = (String) body.getOrDefault(
                "purchaseProofImageUrl",
                "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=600&auto=format&fit=crop&q=80"
        );
        order.setPurchaseProofImageUrl(proofUrl);
        order.setStatus(OrderStatus.PURCHASED_DELIVERING);
        Order savedOrder = orderRepository.save(order);

        // Trừ tiền mặt đang cầm trong túi tài xế (sẽ được hoàn về Ví Tài Xế khi giao xong)
        long reimbursementAmount = savedOrder.getReimbursementAmount();
        driverProfileRepository.findByUserId(driverUser.getId()).ifPresent(profile -> {
            long remaining = Math.max(0L, profile.getCashOnHand() - reimbursementAmount);
            profile.setCashOnHand(remaining);
            driverProfileRepository.save(profile);
        });

        broadcastOrderUpdate(savedOrder);
        return ResponseEntity.ok(savedOrder);
    }

    // 7. CASE B1: TÀI XẾ TỚI NƠI THẤY HÔM NAY NGOẠI NGHỈ -> KHÓA QUÁN TRONG NGÀY & GỬI 3 OPTIONS CHO KHÁCH
    @PostMapping("/driver/orders/{id}/report-vendor-closed")
    @Transactional
    public ResponseEntity<?> reportVendorClosedToday(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        String proofUrl = body != null && body.get("closedProofImageUrl") != null
                ? body.get("closedProofImageUrl").toString()
                : "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600&auto=format&fit=crop&q=80";

        Vendor vendor = order.getVendor();
        vendor.setClosedToday(true);
        vendor.setClosedProofImageUrl(proofUrl);
        vendor.setLastClosedReportAt(LocalDateTime.now());
        vendorRepository.save(vendor);

        order.setVendorClosedProofImageUrl(proofUrl);
        order.setStatus(OrderStatus.VENDOR_CLOSED_WAITING_CUSTOMER);
        order = orderRepository.save(order);

        broadcastOrderUpdate(order);
        return ResponseEntity.ok(order);
    }

    // 8. KHÁCH HÀNG CHỌN 1 TRONG 3 OPTIONS KHI HÔM NAY NGOẠI NGHỈ (CASE B1)
    @PostMapping("/orders/{id}/customer-closed-choice")
    @Transactional
    public ResponseEntity<?> handleCustomerClosedChoice(
            @PathVariable Long id,
            @RequestBody ClosedChoiceRequest req
    ) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        User customer = userRepository.findByIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        String option = req.option() != null ? req.option() : "OPTION_1_REFUND_100";

        if ("OPTION_1_REFUND_100".equals(option)) {
            // Hoàn 100% tiền từ Ví thứ 3 về Ví Khách Hàng + Trích Quỹ rủi ro hỗ trợ 15k xăng cho tài xế
            long refund = order.getTotalEscrowAmount();
            customer.setCustomerWalletBalance(customer.getCustomerWalletBalance() + refund);
            userRepository.save(customer);

            transactionRepository.save(new EscrowTransaction(
                    order.getId(),
                    customer.getId(),
                    TransactionType.REFUND_TO_CUSTOMER_WALLET,
                    refund,
                    "Option 1 (Ngoại nghỉ): Hoàn 100% (" + refund + "đ) về Ví Hoàn Tiền của Khách"
            ));

            if (order.getDriver() != null) {
                User driver = userRepository.findByIdForUpdate(order.getDriver().getId()).orElse(null);
                SystemShiftConfig shift = shiftConfigRepository.findById(1L).orElse(null);
                if (driver != null && shift != null && shift.getRiskFundBalance() >= 15000L) {
                    shift.setRiskFundBalance(shift.getRiskFundBalance() - 15000L);
                    shiftConfigRepository.save(shift);
                    driver.setDriverWalletBalance(driver.getDriverWalletBalance() + 15000L);
                    userRepository.save(driver);

                    transactionRepository.save(new EscrowTransaction(
                            order.getId(),
                            driver.getId(),
                            TransactionType.RISK_FUND_COMPENSATION,
                            15000L,
                            "Quỹ Hỗ Trợ Rủi Ro bù 15.000đ tiền xăng cho Shipper (Đơn #" + order.getId() + " Ngoại nghỉ)"
                    ));
                }
            }

            order.setStatus(OrderStatus.CANCELLED_REFUNDED);
            order = orderRepository.save(order);
            broadcastOrderUpdate(order);
            return ResponseEntity.ok(order);
        }

        if ("OPTION_2_TIP_GAS".equals(option)) {
            // Khách trích tiền tip xăng cho tài xế (VD 15.000đ), phần còn lại hoàn về Ví Khách Hàng
            long gasTip = req.gasTipAmount() != null && req.gasTipAmount() > 0 ? req.gasTipAmount() : 15000L;
            gasTip = Math.min(gasTip, order.getTotalEscrowAmount());
            long refundRest = order.getTotalEscrowAmount() - gasTip;

            customer.setCustomerWalletBalance(customer.getCustomerWalletBalance() + refundRest);
            userRepository.save(customer);

            transactionRepository.save(new EscrowTransaction(
                    order.getId(),
                    customer.getId(),
                    TransactionType.REFUND_TO_CUSTOMER_WALLET,
                    refundRest,
                    "Option 2 (Ngoại nghỉ): Hoàn " + refundRest + "đ về Ví Khách Hàng (đã trích " + gasTip + "đ hỗ trợ xăng cho Shipper)"
            ));

            if (order.getDriver() != null) {
                User driver = userRepository.findByIdForUpdate(order.getDriver().getId()).orElse(null);
                if (driver != null) {
                    driver.setDriverWalletBalance(driver.getDriverWalletBalance() + gasTip);
                    userRepository.save(driver);
                    transactionRepository.save(new EscrowTransaction(
                            order.getId(),
                            driver.getId(),
                            TransactionType.DRIVER_GAS_TIP,
                            gasTip,
                            "Khách hỗ trợ " + gasTip + "đ tiền xăng cho Shipper (Đơn #" + order.getId() + ")"
                    ));
                }
            }

            order.setDriverTipAmount(gasTip);
            order.setStatus(OrderStatus.CANCELLED_REFUNDED);
            order = orderRepository.save(order);
            broadcastOrderUpdate(order);
            return ResponseEntity.ok(order);
        }

        // OPTION 3: ĐỔI SANG MUA CỦA NGOẠI KHÁC BẤT KỲ Ở GẦN VỊ TRÍ SHIPPER (KHÔNG CẦN TRÙNG MÓN)
        Vendor newVendor = vendorRepository.findById(req.newVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Ngoại mới để đổi đơn"));
        VendorDish newDish = (req.newDishId() != null)
                ? dishRepository.findById(req.newDishId()).orElse(newVendor.getDishes().get(0))
                : newVendor.getDishes().get(0);
        int newQty = req.newQuantity() != null && req.newQuantity() > 0 ? req.newQuantity() : 1;

        long oldFoodAmount = order.getFoodAmount();
        long newFoodAmount = newDish.getEstimatedPrice() * newQty;

        order.setVendor(newVendor);
        order.getItems().clear();
        order.addItem(new OrderItem(newDish.getId(), newDish.getName(), newQty, newDish.getEstimatedPrice()));

        if (newFoodAmount <= oldFoodAmount) {
            long excess = oldFoodAmount - newFoodAmount;
            order.setFoodAmount(newFoodAmount);
            order.setTotalEscrowAmount(order.getTotalEscrowAmount() - excess);
            order.setStatus(OrderStatus.DRIVER_ASSIGNED);

            if (excess > 0) {
                customer.setCustomerWalletBalance(customer.getCustomerWalletBalance() + excess);
                userRepository.save(customer);
                transactionRepository.save(new EscrowTransaction(
                        order.getId(),
                        customer.getId(),
                        TransactionType.REFUND_TO_CUSTOMER_WALLET,
                        excess,
                        "Option 3 (Đổi sang " + newVendor.getName() + "): Hoàn " + excess + "đ tiền món chênh lệch dư về Ví Khách Hàng"
                ));
            }
        } else {
            // Món mới đắt hơn món cũ -> Bật đếm ngược 3 phút chờ khách thanh toán bù (nếu huỷ/hết giờ sẽ quay lại Option 1 hoặc 2)
            long diff = newFoodAmount - oldFoodAmount;
            order.setFoodAmount(newFoodAmount);
            order.setPendingTopupDiffAmount(diff);
            order.setTopupDeadlineAt(LocalDateTime.now().plusMinutes(3));
            order.setStatus(OrderStatus.WAITING_TOPUP_DIFF);
        }

        order = orderRepository.save(order);
        broadcastOrderUpdate(order);
        return ResponseEntity.ok(order);
    }

    // 9. XÁC NHẬN THANH TOÁN BÙ CHÊNH LỆCH Ở OPTION 3 HOẶC HUỶ/QUÁ 3 PHÚT QUAY LẠI OPTION 1/2
    @PostMapping("/orders/{id}/confirm-topup-diff")
    @Transactional
    public ResponseEntity<?> confirmTopupDiff(@PathVariable Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.WAITING_TOPUP_DIFF) {
            long diff = order.getPendingTopupDiffAmount();
            order.setTotalEscrowAmount(order.getTotalEscrowAmount() + diff);
            order.setPendingTopupDiffAmount(0L);
            order.setStatus(OrderStatus.DRIVER_ASSIGNED);
            order = orderRepository.save(order);

            transactionRepository.save(new EscrowTransaction(
                    order.getId(),
                    order.getCustomer().getId(),
                    TransactionType.ESCROW_TOPUP_DIFF,
                    diff,
                    "Khách chuyển khoản bù " + diff + "đ chênh lệch khi đổi sang " + order.getVendor().getName()
            ));
            broadcastOrderUpdate(order);
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping("/orders/{id}/cancel-topup-diff")
    @Transactional
    public ResponseEntity<?> cancelOrTimeoutTopupDiff(@PathVariable Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.WAITING_TOPUP_DIFF) {
            // Khôi phục lại số tiền gốc ban đầu và đưa về màn hình chọn Option 1 hoặc Option 2
            long diff = order.getPendingTopupDiffAmount();
            order.setFoodAmount(order.getFoodAmount() - diff);
            order.setPendingTopupDiffAmount(0L);
            order.setTopupDeadlineAt(null);
            if (order.getOriginalVendor() != null) {
                order.setVendor(order.getOriginalVendor());
            }
            order.setStatus(OrderStatus.VENDOR_CLOSED_WAITING_CUSTOMER);
            order = orderRepository.save(order);
            broadcastOrderUpdate(order);
        }
        return ResponseEntity.ok(order);
    }

    // 10. HOÀN THÀNH GIAO HÀNG -> GIẢI NGÂN TỪ VÍ THỨ 3 VỀ VÍ TÀI XẾ & CHIA QUỸ RỦI RO
    @PostMapping("/driver/orders/{id}/complete")
    @Transactional
    public ResponseEntity<?> completeOrder(@PathVariable Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            return ResponseEntity.ok(order);
        }

        order.setStatus(OrderStatus.COMPLETED);
        order = orderRepository.save(order);

        if (order.getDriver() != null) {
            User driver = userRepository.findByIdForUpdate(order.getDriver().getId()).orElse(null);
            if (driver != null) {
                // Tài xế nhận lại 100% tiền món ăn đã ứng trả tiền mặt cho Ngoại + Phí ship + Tip
                long driverPayout = order.getReimbursementAmount() + order.getShippingFee() + order.getDriverTipAmount();
                driver.setDriverWalletBalance(driver.getDriverWalletBalance() + driverPayout);
                userRepository.save(driver);

                transactionRepository.save(new EscrowTransaction(
                        order.getId(),
                        driver.getId(),
                        TransactionType.RELEASE_TO_DRIVER_WALLET,
                        driverPayout,
                        "Giải ngân đơn #" + order.getId() + " về Ví Tài Xế: Hoàn gốc món ăn ("
                                + order.getReimbursementAmount() + "đ) + Ship (" + order.getShippingFee()
                                + "đ) + Tip (" + order.getDriverTipAmount() + "đ)"
                ));
            }
        }

        // Chia 5.000đ phí nền tảng: 3.500đ Doanh thu ròng + 1.500đ vào Quỹ Hỗ Trợ Rủi Ro
        SystemShiftConfig shift = shiftConfigRepository.findById(1L).orElseGet(SystemShiftConfig::new);
        shift.setPlatformRevenueBalance(shift.getPlatformRevenueBalance() + 3500L);
        shift.setRiskFundBalance(shift.getRiskFundBalance() + 1500L);
        shiftConfigRepository.save(shift);

        broadcastOrderUpdate(order);
        return ResponseEntity.ok(order);
    }

    // 11. KHÁCH ĐÁNH GIÁ SAO & BÁO CÁO VI PHẠM CỦA SHIPPER (NÓI XẤU NGOẠI, GIAN LẬN...)
    @PostMapping("/orders/{id}/review")
    @Transactional
    public ResponseEntity<?> reviewDriver(
            @AuthenticationPrincipal User customer,
            @PathVariable Long id,
            @RequestBody ReviewDriverRequest req
    ) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getDriver() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng này chưa có tài xế để đánh giá"));
        }

        DriverReviewReport review = new DriverReviewReport();
        review.setOrderId(order.getId());
        review.setCustomer(customer);
        review.setDriver(order.getDriver());
        review.setRating(req.rating() != null ? req.rating() : 5);
        review.setReportTag(req.reportTag() != null ? req.reportTag() : ReportTag.NONE);
        review.setComment(req.comment());
        DriverReviewReport savedReview = reviewRepository.save(review);

        // Cập nhật điểm trung bình của Shipper
        driverProfileRepository.findByUserId(order.getDriver().getId()).ifPresent(profile -> {
            int count = profile.getTotalRatings() + 1;
            double newAvg = ((profile.getRatingAvg() * profile.getTotalRatings()) + savedReview.getRating()) / count;
            profile.setTotalRatings(count);
            profile.setRatingAvg(Math.round(newAvg * 10.0) / 10.0);
            driverProfileRepository.save(profile);
        });

        return ResponseEntity.ok(Map.of(
                "message", savedReview.getReportTag() != ReportTag.NONE
                        ? "Đã gửi đánh giá và báo cáo vi phạm về Admin xử lý gậy!"
                        : "Cảm ơn bạn đã đánh giá chuyến xe tử tế!",
                "review", savedReview
        ));
    }

    private long calculateFoodAmount(List<OrderItemInput> items) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (OrderItemInput item : items) {
            VendorDish dish = dishRepository.findById(item.dishId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn ID=" + item.dishId()));
            int qty = item.quantity() != null && item.quantity() > 0 ? item.quantity() : 1;
            sum += dish.getEstimatedPrice() * qty;
        }
        return sum;
    }

    private void broadcastOrderUpdate(Order order) {
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), order);
        messagingTemplate.convertAndSend("/topic/orders/updates", order);
    }
}
