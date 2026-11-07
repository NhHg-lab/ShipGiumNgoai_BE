package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.BlacklistAttemptLog;

import java.util.List;

public interface BlacklistAttemptLogRepository extends JpaRepository<BlacklistAttemptLog, Long> {
    List<BlacklistAttemptLog> findAllByOrderByAttemptedAtDesc();
}
