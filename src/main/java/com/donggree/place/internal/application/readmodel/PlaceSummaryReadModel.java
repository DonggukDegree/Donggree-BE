package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;

/** 지도 핀 표시용 장소 요약. 별점은 리뷰가 없으면 null. */
public record PlaceSummaryReadModel(
        Long id,
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude,
        Double averageRating,
        long reviewCount) {

    public PlaceSummaryReadModel withRating(RatingSummary rating) {
        return new PlaceSummaryReadModel(
                id, name, category, subCategory, latitude, longitude, rating.averageRating(), rating.reviewCount());
    }
}
