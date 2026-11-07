package vn.shipgiumngoai.backend.model.entity;

import jakarta.persistence.*;
import vn.shipgiumngoai.backend.model.enums.ReportTag;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_reviews_reports")
public class DriverReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private Integer rating = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportTag reportTag = ReportTag.NONE;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private boolean strikeIssued = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DriverReviewReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ReportTag getReportTag() {
        return reportTag;
    }

    public void setReportTag(ReportTag reportTag) {
        this.reportTag = reportTag;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isStrikeIssued() {
        return strikeIssued;
    }

    public void setStrikeIssued(boolean strikeIssued) {
        this.strikeIssued = strikeIssued;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
