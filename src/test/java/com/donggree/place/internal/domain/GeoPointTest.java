package com.donggree.place.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoPointTest {

    @Test
    void 위도_0_001도는_약_111m다() {
        GeoPoint from = new GeoPoint(37.5582, 126.9983);
        GeoPoint to = new GeoPoint(37.5592, 126.9983);

        assertThat(from.distanceMetersTo(to)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void 사각_범위는_반경을_모두_포함한다() {
        GeoPoint center = new GeoPoint(37.5582, 126.9983);
        GeoPoint.BoundingBox box = center.boundingBox(50);

        GeoPoint north = new GeoPoint(box.maxLatitude(), center.longitude());
        GeoPoint east = new GeoPoint(center.latitude(), box.maxLongitude());
        assertThat(center.distanceMetersTo(north)).isCloseTo(50, within(0.5));
        assertThat(center.distanceMetersTo(east)).isCloseTo(50, within(0.5));
    }

    @Test
    void 범위를_벗어난_좌표는_만들_수_없다() {
        assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0, -181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 캠퍼스_반경_1_5km_안만_서비스_영역이다() {
        assertThat(CampusArea.contains(new GeoPoint(37.5600, 127.0010))).isTrue();
        assertThat(CampusArea.contains(new GeoPoint(37.5800, 126.9983))).isFalse();
    }
}
