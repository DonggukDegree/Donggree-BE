package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;

/** 지도 핀 한 개. 카테고리 칩 필터는 FE가 이 목록에서 바로 거른다. */
public record PlaceSummaryResponse(
        Long id, String name, PlaceCategory category, PlaceSubCategory subCategory, double latitude, double longitude) {

    public static PlaceSummaryResponse from(PlaceSummaryReadModel place) {
        return new PlaceSummaryResponse(
                place.id(), place.name(), place.category(), place.subCategory(), place.latitude(), place.longitude());
    }
}
