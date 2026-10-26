package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;

/** 지도 핀 표시용 장소 요약. */
public record PlaceSummaryReadModel(
        Long id,
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude) {}
