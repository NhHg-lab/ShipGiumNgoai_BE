package vn.shipgiumngoai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.shipgiumngoai.backend.model.entity.SystemShiftConfig;
import vn.shipgiumngoai.backend.model.entity.Vendor;
import vn.shipgiumngoai.backend.model.entity.VendorDish;
import vn.shipgiumngoai.backend.repository.SystemShiftConfigRepository;
import vn.shipgiumngoai.backend.repository.VendorRepository;
import vn.shipgiumngoai.backend.util.GeoUtils;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class VendorController {

    private final VendorRepository vendorRepository;
    private final SystemShiftConfigRepository shiftConfigRepository;

    public VendorController(VendorRepository vendorRepository, SystemShiftConfigRepository shiftConfigRepository) {
        this.vendorRepository = vendorRepository;
        this.shiftConfigRepository = shiftConfigRepository;
    }

    public record DishInput(String name, Long estimatedPrice, String imageUrl) {
    }

    public record ContributeVendorRequest(
            String name,
            String story,
            String addressText,
            Double latitude,
            Double longitude,
            String sellingHours,
            String contributorName,
            Boolean anonymousContributor,
            String sourceVideoUrl,
            List<DishInput> dishes
    ) {
    }

    @GetMapping("/vendors")
    public ResponseEntity<?> getApprovedVendors(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm
    ) {
        List<Vendor> vendors = vendorRepository.findByApprovalStatusOrderByIdAsc("APPROVED");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vendor v : vendors) {
            Double dist = null;
            if (lat != null && lng != null) {
                dist = GeoUtils.distanceKm(lat, lng, v.getLatitude(), v.getLongitude());
                if (radiusKm != null && dist > radiusKm) {
                    continue;
                }
            }
            result.add(toVendorDto(v, dist));
        }

        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(m -> (Double) m.getOrDefault("distanceKm", 999.0)));
        }
        return ResponseEntity.ok(result);
    }

    // Gợi ý các Ngoại khác đang mở bán gần vị trí Shipper (Case B1 - Option 3: Không cần trùng món ăn!)
    @GetMapping("/vendors/{closedVendorId}/nearby-alternatives")
    public ResponseEntity<?> getNearbyAlternatives(
            @PathVariable Long closedVendorId,
            @RequestParam(defaultValue = "2.5") double radiusKm
    ) {
        Optional<Vendor> closedOpt = vendorRepository.findById(closedVendorId);
        if (closedOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Vendor closedVendor = closedOpt.get();
        List<Vendor> allApproved = vendorRepository.findByApprovalStatusOrderByIdAsc("APPROVED");

        List<Map<String, Object>> alternatives = allApproved.stream()
                .filter(v -> !v.getId().equals(closedVendorId) && !v.isClosedToday())
                .map(v -> {
                    double d = GeoUtils.distanceKm(
                            closedVendor.getLatitude(),
                            closedVendor.getLongitude(),
                            v.getLatitude(),
                            v.getLongitude()
                    );
                    return toVendorDto(v, d);
                })
                .filter(m -> (Double) m.get("distanceKm") <= radiusKm)
                .sorted(Comparator.comparing(m -> (Double) m.get("distanceKm")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(alternatives);
    }

    @PostMapping("/vendors/contribute")
    public ResponseEntity<?> contributeVendor(@RequestBody ContributeVendorRequest req) {
        if (req.name() == null || req.name().isBlank() || req.addressText() == null || req.addressText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập tên của Ngoại và địa chỉ điểm bán"));
        }

        Vendor v = new Vendor();
        v.setName(req.name().trim());
        v.setStory(req.story() != null && !req.story().isBlank() ? req.story().trim() : "Cụ lớn tuổi bán hàng rong mưu sinh mỗi tối.");
        v.setAddressText(req.addressText().trim());
        v.setLatitude(req.latitude() != null ? req.latitude() : 10.7765);
        v.setLongitude(req.longitude() != null ? req.longitude() : 106.6950);
        v.setSellingHours(req.sellingHours() != null && !req.sellingHours().isBlank() ? req.sellingHours() : "18:00 - 23:00");

        boolean isAnon = Boolean.TRUE.equals(req.anonymousContributor())
                || req.contributorName() == null
                || req.contributorName().isBlank();
        v.setAnonymousContributor(isAnon);
        v.setContributorName(isAnon ? null : req.contributorName().trim());
        v.setSourceVideoUrl(req.sourceVideoUrl());
        v.setApprovalStatus("PENDING");

        if (req.dishes() != null && !req.dishes().isEmpty()) {
            for (DishInput d : req.dishes()) {
                v.addDish(new VendorDish(
                        d.name(),
                        d.estimatedPrice() != null ? d.estimatedPrice() : 20000L,
                        d.imageUrl()
                ));
            }
        } else {
            v.addDish(new VendorDish("Phần ăn ủng hộ Ngoại", 25000L, null));
        }

        v = vendorRepository.save(v);
        return ResponseEntity.ok(Map.of(
                "message", "Cảm ơn bạn đã gieo duyên! Thông tin của Ngoại đã được gửi cho Admin kiểm duyệt.",
                "vendor", toVendorDto(v, null)
        ));
    }

    @GetMapping("/shift-status")
    public ResponseEntity<?> getShiftStatus() {
        SystemShiftConfig cfg = shiftConfigRepository.findById(1L).orElseGet(() -> {
            SystemShiftConfig s = new SystemShiftConfig();
            return shiftConfigRepository.save(s);
        });
        return ResponseEntity.ok(cfg);
    }

    @PostMapping("/shift-status/subscribe")
    public ResponseEntity<?> subscribeShiftNotification() {
        SystemShiftConfig cfg = shiftConfigRepository.findById(1L).orElseGet(SystemShiftConfig::new);
        cfg.setWaitingNotificationSubscribers(cfg.getWaitingNotificationSubscribers() + 1);
        shiftConfigRepository.save(cfg);
        return ResponseEntity.ok(Map.of(
                "message", "🔔 Đã đăng ký! Ngay khi tụi mình mở ca ship tối nay, hệ thống sẽ báo cho bạn liền nhé!",
                "subscribersCount", cfg.getWaitingNotificationSubscribers()
        ));
    }

    private Map<String, Object> toVendorDto(Vendor v, Double distanceKm) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", v.getId());
        map.put("name", v.getName());
        map.put("story", v.getStory());
        map.put("addressText", v.getAddressText());
        map.put("latitude", v.getLatitude());
        map.put("longitude", v.getLongitude());
        map.put("sellingHours", v.getSellingHours());
        map.put("avatarUrl", v.getAvatarUrl());
        map.put("displayContributor", v.getDisplayContributor());
        map.put("anonymousContributor", v.isAnonymousContributor());
        map.put("sourceVideoUrl", v.getSourceVideoUrl());
        map.put("approvalStatus", v.getApprovalStatus());
        map.put("closedToday", v.isClosedToday());
        map.put("closedProofImageUrl", v.getClosedProofImageUrl());
        map.put("dishes", v.getDishes());
        if (distanceKm != null) {
            map.put("distanceKm", distanceKm);
        }
        return map;
    }
}
