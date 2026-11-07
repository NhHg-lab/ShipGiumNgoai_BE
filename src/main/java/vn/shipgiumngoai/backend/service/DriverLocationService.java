package vn.shipgiumngoai.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.shipgiumngoai.backend.repository.DriverProfileRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DriverLocationService {

    private static final String REDIS_GEO_KEY = "driver_locations";

    private final SimpMessagingTemplate messagingTemplate;
    private final DriverProfileRepository driverProfileRepository;
    private final StringRedisTemplate redisTemplate;

    private final Map<Long, Map<String, Object>> inMemoryLocations = new ConcurrentHashMap<>();

    public DriverLocationService(
            SimpMessagingTemplate messagingTemplate,
            DriverProfileRepository driverProfileRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.messagingTemplate = messagingTemplate;
        this.driverProfileRepository = driverProfileRepository;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> updateDriverLocation(Long driverUserId, Long activeOrderId, double lat, double lng) {
        // 1. Lưu vào Redis GEO (nếu Redis đang chạy)
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForGeo().add(REDIS_GEO_KEY, new Point(lng, lat), String.valueOf(driverUserId));
            } catch (Exception ignored) {
                // Fallback sang RAM nếu chưa bật Redis container
            }
        }

        // 2. Lưu vào RAM cache cực nhanh
        Map<String, Object> payload = Map.of(
                "driverId", driverUserId,
                "orderId", activeOrderId != null ? activeOrderId : 0L,
                "lat", lat,
                "lng", lng,
                "timestamp", LocalDateTime.now().toString()
        );
        inMemoryLocations.put(driverUserId, payload);

        // 3. Cập nhật vị trí gần nhất vào DriverProfile
        driverProfileRepository.findByUserId(driverUserId).ifPresent(profile -> {
            profile.setCurrentLat(lat);
            profile.setCurrentLng(lng);
            profile.setLastLocationUpdateAt(LocalDateTime.now());
            driverProfileRepository.save(profile);
        });

        // 4. Bắn qua WebSocket STOMP tới màn hình Khách hàng đang theo dõi đơn
        if (activeOrderId != null && activeOrderId > 0) {
            messagingTemplate.convertAndSend("/topic/orders/" + activeOrderId + "/location", payload);
        }
        messagingTemplate.convertAndSend("/topic/drivers/locations", payload);

        return payload;
    }

    public Map<String, Object> getDriverLocation(Long driverUserId) {
        return inMemoryLocations.get(driverUserId);
    }
}
