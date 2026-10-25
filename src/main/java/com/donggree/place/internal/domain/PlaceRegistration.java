package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
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
        List<MenuItem> menus) {}
