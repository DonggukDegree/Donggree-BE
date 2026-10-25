package com.donggree.place.internal.domain;

/**
 * 사용자가 지도 중심 핀으로 지정한 WGS84 좌표.
 * 캠퍼스 주변의 짧은 거리만 다루므로 PostGIS 없이 하버사인 거리로 계산한다.
 */
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320.0;

    public GeoPoint {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }

    /** 두 좌표 사이의 지표면 거리(m). */
    public double distanceMetersTo(GeoPoint other) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(other.longitude - longitude);
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(haversine)));
    }

    /**
     * 반경 후보를 DB에서 좁히기 위한 사각 범위. 정확한 거리 판정은 {@link #distanceMetersTo}로 다시 한다.
     * 극지방을 다루지 않으므로 경도 보정은 현재 위도의 코사인으로 충분하다.
     */
    public BoundingBox boundingBox(double radiusMeters) {
        double latitudeDelta = radiusMeters / METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta = radiusMeters / (METERS_PER_LATITUDE_DEGREE * Math.cos(Math.toRadians(latitude)));
        return new BoundingBox(
                latitude - latitudeDelta,
                latitude + latitudeDelta,
                longitude - longitudeDelta,
                longitude + longitudeDelta);
    }

    public record BoundingBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {}
}
