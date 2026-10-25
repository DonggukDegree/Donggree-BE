package com.donggree.place.internal.domain;

/** 동글맵 서비스 영역. 동국대학교 서울캠퍼스 중심 반경 1.5km 안에서만 장소를 등록한다. */
public final class CampusArea {

    public static final GeoPoint CENTER = new GeoPoint(37.55828693771417, 127.00018391368702);
    public static final double RADIUS_METERS = 1_500;

    private CampusArea() {}

    public static boolean contains(GeoPoint point) {
        return CENTER.distanceMetersTo(point) <= RADIUS_METERS;
    }
}
