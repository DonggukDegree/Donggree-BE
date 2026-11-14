package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.List;

/** 지도 핀 표시용 장소 요약. 별점은 리뷰가 없으면 null, 태그는 숨은 맛집 칩 필터·핀 표시에 쓴다. */
public record PlaceSummaryReadModel(
        Long id,
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude,
        Double averageRating,
        long reviewCount,
        List<PlaceTag> tags) {

    public PlaceSummaryReadModel {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public PlaceSummaryReadModel withRatingAndTags(RatingSummary rating, List<PlaceTag> placeTags) {
        return new PlaceSummaryReadModel(
                id,
                name,
                category,
                subCategory,
                latitude,
                longitude,
                rating.averageRating(),
                rating.reviewCount(),
                placeTags);
    }
}
