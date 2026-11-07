package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.WithdrawalRequest;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
