package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.DriverBlacklist;

import java.util.Optional;

public interface DriverBlacklistRepository extends JpaRepository<DriverBlacklist, Long> {
    Optional<DriverBlacklist> findByCccdNumberOrPhone(String cccdNumber, String phone);
    boolean existsByCccdNumber(String cccdNumber);
}
