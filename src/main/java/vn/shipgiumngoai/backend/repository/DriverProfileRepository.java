package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.DriverProfile;
import vn.shipgiumngoai.backend.model.enums.VerificationStatus;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);
    List<DriverProfile> findByVerificationStatus(VerificationStatus status);
}
