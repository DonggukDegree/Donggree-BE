package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** 장소 테스트 공용 생성기. 캠퍼스 중심 근처 좌표를 기본으로 쓴다. */
public final class PlaceFixtures {

    public static final GeoPoint NEAR_CAMPUS = new GeoPoint(37.5590, 126.9990);

    private PlaceFixtures() {}

    public static PlaceRegistration registration(String name, GeoPoint point) {
        return new PlaceRegistration(
                1L,
                name,
                PlaceCategory.RESTAURANT,
                PlaceSubCategory.CHINESE,
                point,
                "평일 11:00-21:00",
                "정문 맞은편",
                List.of(new MenuItem("짜장면", 7000), new MenuItem("탕수육", null)));
    }

    public static Place place(Long id, String name, GeoPoint point) {
        Place place = Place.register(registration(name, point));
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }
}
