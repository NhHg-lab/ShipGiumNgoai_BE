package vn.shipgiumngoai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.shipgiumngoai.backend.model.entity.User;
import vn.shipgiumngoai.backend.service.DriverLocationService;

import java.util.Map;

@RestController
public class LocationTrackingController {

    private final DriverLocationService driverLocationService;

    public LocationTrackingController(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    public record LocationUpdateMessage(Long driverId, Long orderId, Double lat, Double lng) {
    }

    // Nhận tọa độ GPS từ WebSocket STOMP (/app/driver/location)
    @MessageMapping("/driver/location")
    public void handleWebSocketLocation(@Payload LocationUpdateMessage msg) {
        if (msg.driverId() != null && msg.lat() != null && msg.lng() != null) {
            driverLocationService.updateDriverLocation(msg.driverId(), msg.orderId(), msg.lat(), msg.lng());
        }
    }

    // Nhận tọa độ GPS qua REST API (fallback tiện lợi cho cả Web & Flutter)
    @PostMapping("/api/driver/location")
    public ResponseEntity<?> updateLocationRest(
            @AuthenticationPrincipal User user,
            @RequestBody LocationUpdateMessage msg
    ) {
        Long driverId = user != null ? user.getId() : msg.driverId();
        Map<String, Object> result = driverLocationService.updateDriverLocation(
                driverId,
                msg.orderId(),
                msg.lat(),
                msg.lng()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/public/drivers/{driverId}/location")
    public ResponseEntity<?> getDriverLatestLocation(@PathVariable Long driverId) {
        Map<String, Object> loc = driverLocationService.getDriverLocation(driverId);
        if (loc == null) {
            return ResponseEntity.ok(Map.of("driverId", driverId, "available", false));
        }
        return ResponseEntity.ok(loc);
    }
}
