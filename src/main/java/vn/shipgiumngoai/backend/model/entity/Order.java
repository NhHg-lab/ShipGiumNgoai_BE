package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;
import vn.shipgiumngoai.backend.model.enums.DeliveryType;
import vn.shipgiumngoai.backend.model.enums.OrderStatus;
import vn.shipgiumngoai.backend.model.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "original_vendor_id")
    private Vendor originalVendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryType deliveryType = DeliveryType.INSTANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PaymentMethod paymentMethod = PaymentMethod.VIETQR;

    @Column(nullable = false, length = 300)
    private String deliveryAddress;

    @Column(nullable = false)
    private Double deliveryLat;

    @Column(nullable = false)
    private Double deliveryLng;

    @Column(nullable = false)
    private Double distanceKm;

    // 1. Tiền món ăn thu hộ (Trả tiền mặt cho Ngoại 100%, không chịu thuế nền tảng)
    @Column(nullable = false)
    private Long foodAmount = 0L;

    // Tiền khách biếu thêm cho Ngoại (Trả tiền mặt cho Ngoại 100%)
    @Column(nullable = false)
    private Long vendorGiftAmount = 0L;

    // 2. Phí giao hàng (2km đầu 15k/12k + 4.5k/km)
    @Column(nullable = false)
    private Long shippingFee = 0L;

    // 3. Phí nền tảng (5.000đ: gồm 3.500đ doanh thu nền tảng + 1.500đ Quỹ hỗ trợ rủi ro)
    @Column(nullable = false)
    private Long platformFee = 5000L;

    // 4. Tiền Tip cho Shipper (hoặc tiền hỗ trợ xăng Option 2 khi Ngoại nghỉ)
    @Column(nullable = false)
    private Long driverTipAmount = 0L;

    // Tổng tiền đang giữ trong Ví thứ 3 (Escrow)
    @Column(nullable = false)
    private Long totalEscrowAmount = 0L;

    // Phần tiền chênh lệch cần bù khi chọn Option 3 đổi sang món đắt hơn
    @Column(nullable = false)
    private Long pendingTopupDiffAmount = 0L;

    private LocalDateTime topupDeadlineAt;

    @Column(length = 500)
    private String purchaseProofImageUrl;

    @Column(length = 500)
    private String vendorClosedProofImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Order() {
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getReimbursementAmount() {
        return (foodAmount != null ? foodAmount : 0L) + (vendorGiftAmount != null ? vendorGiftAmount : 0L);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Vendor getOriginalVendor() {
        return originalVendor;
    }

    public void setOriginalVendor(Vendor originalVendor) {
        this.originalVendor = originalVendor;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Double getDeliveryLat() {
        return deliveryLat;
    }

    public void setDeliveryLat(Double deliveryLat) {
        this.deliveryLat = deliveryLat;
    }

    public Double getDeliveryLng() {
        return deliveryLng;
    }

    public void setDeliveryLng(Double deliveryLng) {
        this.deliveryLng = deliveryLng;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Long getFoodAmount() {
        return foodAmount;
    }

    public void setFoodAmount(Long foodAmount) {
        this.foodAmount = foodAmount;
    }

    public Long getVendorGiftAmount() {
        return vendorGiftAmount;
    }

    public void setVendorGiftAmount(Long vendorGiftAmount) {
        this.vendorGiftAmount = vendorGiftAmount;
    }

    public Long getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Long shippingFee) {
        this.shippingFee = shippingFee;
    }

    public Long getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(Long platformFee) {
        this.platformFee = platformFee;
    }

    public Long getDriverTipAmount() {
        return driverTipAmount;
    }

    public void setDriverTipAmount(Long driverTipAmount) {
        this.driverTipAmount = driverTipAmount;
    }

    public Long getTotalEscrowAmount() {
        return totalEscrowAmount;
    }

    public void setTotalEscrowAmount(Long totalEscrowAmount) {
        this.totalEscrowAmount = totalEscrowAmount;
    }

    public Long getPendingTopupDiffAmount() {
        return pendingTopupDiffAmount;
    }

    public void setPendingTopupDiffAmount(Long pendingTopupDiffAmount) {
        this.pendingTopupDiffAmount = pendingTopupDiffAmount;
    }

    public LocalDateTime getTopupDeadlineAt() {
        return topupDeadlineAt;
    }

    public void setTopupDeadlineAt(LocalDateTime topupDeadlineAt) {
        this.topupDeadlineAt = topupDeadlineAt;
    }

    public String getPurchaseProofImageUrl() {
        return purchaseProofImageUrl;
    }

    public void setPurchaseProofImageUrl(String purchaseProofImageUrl) {
        this.purchaseProofImageUrl = purchaseProofImageUrl;
    }

    public String getVendorClosedProofImageUrl() {
        return vendorClosedProofImageUrl;
    }

    public void setVendorClosedProofImageUrl(String vendorClosedProofImageUrl) {
        this.vendorClosedProofImageUrl = vendorClosedProofImageUrl;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
