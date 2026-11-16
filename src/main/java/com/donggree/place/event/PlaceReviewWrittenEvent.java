package com.donggree.place.event;

/**
 * 회원이 리뷰를 작성했다.
 *
 * @param firstReviewOfPlace 작성 시점에 그 장소의 다른 리뷰가 하나도 없었는지(지운·숨긴 리뷰도 있었던 것으로 본다)
 */
public record PlaceReviewWrittenEvent(Long reviewId, Long placeId, Long memberId, boolean firstReviewOfPlace) {}
