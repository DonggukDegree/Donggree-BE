package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.List;

/** 장소 등록 입력값. 엔티티 생성 파라미터를 묶는 도메인 파라미터 객체. */
public record PlaceRegistration(
        Long memberId,
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        GeoPoint point,
        String businessHours,
        String description,
        List<MenuItem> menus,
        List<PlaceTag> tags) {

    /** 숨은 맛집 태그 없이 등록한다. */
    public PlaceRegistration(
            Long memberId,
            String name,
            PlaceCategory category,
            PlaceSubCategory subCategory,
            GeoPoint point,
            String businessHours,
            String description,
            List<MenuItem> menus) {
        this(memberId, name, category, subCategory, point, businessHours, description, menus, List.of());
    }
}
