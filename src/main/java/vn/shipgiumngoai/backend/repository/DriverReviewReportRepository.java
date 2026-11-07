package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.DriverReviewReport;

import java.util.List;

public interface DriverReviewReportRepository extends JpaRepository<DriverReviewReport, Long> {
    List<DriverReviewReport> findAllByOrderByCreatedAtDesc();
    List<DriverReviewReport> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
