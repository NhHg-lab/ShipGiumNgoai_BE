package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String story;

    @Column(nullable = false, length = 300)
    private String addressText;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 100)
    private String sellingHours = "18:00 - 23:00";

    @Column(length = 500)
    private String avatarUrl;

    // Tính năng "Tôn vinh Người Gieo Duyên" (tùy tâm, nếu ẩn danh hoặc trống thì hiện "Một người tốt ẩn danh")
    @Column(length = 120)
    private String contributorName;

    @Column(nullable = false)
    private boolean anonymousContributor = false;

    @Column(length = 500)
    private String sourceVideoUrl;

    @Column(nullable = false, length = 20)
    private String approvalStatus = "APPROVED"; // PENDING, APPROVED, REJECTED

    // Trạng thái Ngoại nghỉ hôm nay (chỉ cập nhật khi Shipper tới nơi xác nhận)
    @Column(nullable = false)
    private boolean closedToday = false;

    @Column(length = 500)
    private String closedProofImageUrl;

    private LocalDateTime lastClosedReportAt;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VendorDish> dishes = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Vendor() {
    }

    public String getDisplayContributor() {
        if (anonymousContributor || contributorName == null || contributorName.isBlank()) {
            return "Một người tốt ẩn danh";
        }
        return contributorName.trim();
    }

    public void addDish(VendorDish dish) {
        dishes.add(dish);
        dish.setVendor(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getSellingHours() {
        return sellingHours;
    }

    public void setSellingHours(String sellingHours) {
        this.sellingHours = sellingHours;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getContributorName() {
        return contributorName;
    }

    public void setContributorName(String contributorName) {
        this.contributorName = contributorName;
    }

    public boolean isAnonymousContributor() {
        return anonymousContributor;
    }

    public void setAnonymousContributor(boolean anonymousContributor) {
        this.anonymousContributor = anonymousContributor;
    }

    public String getSourceVideoUrl() {
        return sourceVideoUrl;
    }

    public void setSourceVideoUrl(String sourceVideoUrl) {
        this.sourceVideoUrl = sourceVideoUrl;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public boolean isClosedToday() {
        return closedToday;
    }

    public void setClosedToday(boolean closedToday) {
        this.closedToday = closedToday;
    }

    public String getClosedProofImageUrl() {
        return closedProofImageUrl;
    }

    public void setClosedProofImageUrl(String closedProofImageUrl) {
        this.closedProofImageUrl = closedProofImageUrl;
    }

    public LocalDateTime getLastClosedReportAt() {
        return lastClosedReportAt;
    }

    public void setLastClosedReportAt(LocalDateTime lastClosedReportAt) {
        this.lastClosedReportAt = lastClosedReportAt;
    }

    public List<VendorDish> getDishes() {
        return dishes;
    }

    public void setDishes(List<VendorDish> dishes) {
        this.dishes = dishes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
