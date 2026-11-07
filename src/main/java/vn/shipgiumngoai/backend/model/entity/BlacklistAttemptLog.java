package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklist_attempt_logs")
public class BlacklistAttemptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 25)
    private String cccdNumber;

    @Column(nullable = false, length = 20)
    private String attemptedPhone;

    @Column(nullable = false, length = 100)
    private String attemptedFullName;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 500)
    private String cccdFrontSnapshot;

    @Column(length = 500)
    private String alertMessage;

    @Column(nullable = false)
    private LocalDateTime attemptedAt = LocalDateTime.now();

    public BlacklistAttemptLog() {
    }

    public BlacklistAttemptLog(String cccdNumber, String attemptedPhone, String attemptedFullName, String ipAddress, String cccdFrontSnapshot, String alertMessage) {
        this.cccdNumber = cccdNumber;
        this.attemptedPhone = attemptedPhone;
        this.attemptedFullName = attemptedFullName;
        this.ipAddress = ipAddress;
        this.cccdFrontSnapshot = cccdFrontSnapshot;
        this.alertMessage = alertMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCccdNumber() {
        return cccdNumber;
    }

    public void setCccdNumber(String cccdNumber) {
        this.cccdNumber = cccdNumber;
    }

    public String getAttemptedPhone() {
        return attemptedPhone;
    }

    public void setAttemptedPhone(String attemptedPhone) {
        this.attemptedPhone = attemptedPhone;
    }

    public String getAttemptedFullName() {
        return attemptedFullName;
    }

    public void setAttemptedFullName(String attemptedFullName) {
        this.attemptedFullName = attemptedFullName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCccdFrontSnapshot() {
        return cccdFrontSnapshot;
    }

    public void setCccdFrontSnapshot(String cccdFrontSnapshot) {
        this.cccdFrontSnapshot = cccdFrontSnapshot;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
