package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.EscrowTransaction;

import java.util.List;

public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {
    List<EscrowTransaction> findAllByOrderByCreatedAtDesc();
    List<EscrowTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
