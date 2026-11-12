package com.donggree.place.internal.application.readmodel;

import java.util.List;

/**
 * 리뷰 목록 한 페이지(최신순). 다음 페이지는 nextCursor를 cursor로 넘겨 받는다.
 *
 * @param nextCursor 다음 페이지 커서. 마지막 페이지면 null
 * @param myReview   요청 회원의 리뷰(숨겨졌어도 포함). 없으면 null
 */
public record PlaceReviewPageReadModel(
        List<PlaceReviewReadModel> reviews, Long nextCursor, PlaceReviewReadModel myReview) {}
