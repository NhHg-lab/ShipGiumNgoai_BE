package vn.shipgiumngoai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shipgiumngoai.backend.model.entity.VendorDish;

import java.util.List;

public interface VendorDishRepository extends JpaRepository<VendorDish, Long> {
    List<VendorDish> findByVendorId(Long vendorId);
}
