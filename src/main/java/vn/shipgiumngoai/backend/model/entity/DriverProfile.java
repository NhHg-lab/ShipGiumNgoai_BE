package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;
import vn.shipgiumngoai.backend.model.enums.VerificationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 25)
    private String cccdNumber;

    @Column(nullable = false)
    private String cccdFrontPath;

    @Column(nullable = false)
    private String cccdBackPath;

    @Column(nullable = false, length = 30)
    private String vehiclePlate;

    // Số tiền mặt đang mang trong người (để lọc đơn Giao Nhanh INSTANT)
    @Column(nullable = false)
    private Long cashOnHand = 300000L;

    @Column(nullable = false)
    private boolean online = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_INTERVIEW;

    @Column(length = 500)
    private String interviewNotes;

    @Column(nullable = false)
    private Double ratingAvg = 5.0;

    @Column(nullable = false)
    private Integer totalRatings = 0;

    @Column(nullable = false)
    private Integer strikeCount = 0;

    @Column(nullable = false)
    private boolean agreedCashTerms = true;

    private Double currentLat;

    private Double currentLng;

    private LocalDateTime lastLocationUpdateAt;

    public DriverProfile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCccdNumber() {
        return cccdNumber;
    }

    public void setCccdNumber(String cccdNumber) {
        this.cccdNumber = cccdNumber;
    }

    public String getCccdFrontPath() {
        return cccdFrontPath;
    }

    public void setCccdFrontPath(String cccdFrontPath) {
        this.cccdFrontPath = cccdFrontPath;
    }

    public String getCccdBackPath() {
        return cccdBackPath;
    }

    public void setCccdBackPath(String cccdBackPath) {
        this.cccdBackPath = cccdBackPath;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public Long getCashOnHand() {
        return cashOnHand;
    }

    public void setCashOnHand(Long cashOnHand) {
        this.cashOnHand = cashOnHand;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getInterviewNotes() {
        return interviewNotes;
    }

    public void setInterviewNotes(String interviewNotes) {
        this.interviewNotes = interviewNotes;
    }

    public Double getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(Double ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Integer getStrikeCount() {
        return strikeCount;
    }

    public void setStrikeCount(Integer strikeCount) {
        this.strikeCount = strikeCount;
    }

    public boolean isAgreedCashTerms() {
        return agreedCashTerms;
    }

    public void setAgreedCashTerms(boolean agreedCashTerms) {
        this.agreedCashTerms = agreedCashTerms;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(Double currentLng) {
        this.currentLng = currentLng;
    }

    public LocalDateTime getLastLocationUpdateAt() {
        return lastLocationUpdateAt;
    }

    public void setLastLocationUpdateAt(LocalDateTime lastLocationUpdateAt) {
        this.lastLocationUpdateAt = lastLocationUpdateAt;
    }
}
