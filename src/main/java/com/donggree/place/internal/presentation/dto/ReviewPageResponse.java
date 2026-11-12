package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceReviewPageReadModel;
import java.util.List;

/** 리뷰 목록 한 페이지. 다음 페이지는 nextCursor를 cursor로 넘긴다(마지막이면 null). */
public record ReviewPageResponse(List<ReviewResponse> reviews, Long nextCursor, ReviewResponse myReview) {

    public static ReviewPageResponse from(PlaceReviewPageReadModel page) {
        return new ReviewPageResponse(
                page.reviews().stream().map(ReviewResponse::from).toList(),
                page.nextCursor(),
                page.myReview() == null ? null : ReviewResponse.from(page.myReview()));
    }
}
