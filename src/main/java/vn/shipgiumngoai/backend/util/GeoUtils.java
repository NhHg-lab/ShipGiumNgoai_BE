package vn.shipgiumngoai.backend.util;

import vn.shipgiumngoai.backend.model.enums.DeliveryType;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        // Nhân hệ số đường thực tế đô thị 1.25 để sát với quãng đường chạy xe ngoài đời
        double rawKm = EARTH_RADIUS_KM * c * 1.25;
        return Math.round(rawKm * 100.0) / 100.0;
    }

    public static long calculateShippingFee(double distanceKm, DeliveryType deliveryType) {
        long baseFee = (deliveryType == DeliveryType.FLEXIBLE) ? 12000L : 15000L;
        if (distanceKm <= 2.0) {
            return baseFee;
        }
        double extraKm = distanceKm - 2.0;
        long extraFee = Math.round(Math.ceil(extraKm * 10.0) / 10.0 * 4500.0);
        // Làm tròn đến hàng nghìn gần nhất
        return ((baseFee + extraFee + 500) / 1000) * 1000;
    }
}
