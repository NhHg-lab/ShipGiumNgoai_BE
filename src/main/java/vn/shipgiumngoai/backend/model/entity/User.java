package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;
import vn.shipgiumngoai.backend.model.enums.Role;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private boolean banned = false;

    // Ví hoàn tiền của Khách hàng (chỉ nhận tiền hoàn đơn, dùng mua đơn mới hoặc rút về ngân hàng)
    @Column(nullable = false)
    private Long customerWalletBalance = 0L;

    // Ví đối soát của Tài xế (nhận tiền gốc món ăn đã ứng + phí ship + tip)
    @Column(nullable = false)
    private Long driverWalletBalance = 0L;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {
    }

    public User(String phone, String fullName, String passwordHash, Role role) {
        this.phone = phone;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public Long getCustomerWalletBalance() {
        return customerWalletBalance;
    }

    public void setCustomerWalletBalance(Long customerWalletBalance) {
        this.customerWalletBalance = customerWalletBalance;
    }

    public Long getDriverWalletBalance() {
        return driverWalletBalance;
    }

    public void setDriverWalletBalance(Long driverWalletBalance) {
        this.driverWalletBalance = driverWalletBalance;
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
