package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_shift_config")
public class SystemShiftConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean shiftOpen = true;

    @Column(nullable = false, length = 50)
    private String defaultHours = "18:00 - 23:00";

    @Column(nullable = false, length = 50)
    private String todayCustomHours = "18:00 - 23:00";

    @Column(nullable = false, length = 500)
    private String statusBannerMessage = "🟢 Đội ngũ sinh viên Ship Giùm Ngoại đang mở ca chạy (18:00 - 23:00)! Đặt đơn ủng hộ Ngoại ngay nhé!";

    // Quỹ hỗ trợ rủi ro (trích 1.500đ từ mỗi đơn thành công để bù xăng khi Ngoại nghỉ)
    @Column(nullable = false)
    private Long riskFundBalance = 150000L;

    // Doanh thu ròng của nền tảng (3.500đ/đơn)
    @Column(nullable = false)
    private Long platformRevenueBalance = 0L;

    @Column(nullable = false)
    private Integer waitingNotificationSubscribers = 0;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SystemShiftConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isShiftOpen() {
        return shiftOpen;
    }

    public void setShiftOpen(boolean shiftOpen) {
        this.shiftOpen = shiftOpen;
    }

    public String getDefaultHours() {
        return defaultHours;
    }

    public void setDefaultHours(String defaultHours) {
        this.defaultHours = defaultHours;
    }

    public String getTodayCustomHours() {
        return todayCustomHours;
    }

    public void setTodayCustomHours(String todayCustomHours) {
        this.todayCustomHours = todayCustomHours;
    }

    public String getStatusBannerMessage() {
        return statusBannerMessage;
    }

    public void setStatusBannerMessage(String statusBannerMessage) {
        this.statusBannerMessage = statusBannerMessage;
    }

    public Long getRiskFundBalance() {
        return riskFundBalance;
    }

    public void setRiskFundBalance(Long riskFundBalance) {
        this.riskFundBalance = riskFundBalance;
    }

    public Long getPlatformRevenueBalance() {
        return platformRevenueBalance;
    }

    public void setPlatformRevenueBalance(Long platformRevenueBalance) {
        this.platformRevenueBalance = platformRevenueBalance;
    }

    public Integer getWaitingNotificationSubscribers() {
        return waitingNotificationSubscribers;
    }

    public void setWaitingNotificationSubscribers(Integer waitingNotificationSubscribers) {
        this.waitingNotificationSubscribers = waitingNotificationSubscribers;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
