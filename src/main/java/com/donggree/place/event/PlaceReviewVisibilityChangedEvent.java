package com.donggree.place.event;

/**
 * 관리자 처리나 신고 누적으로 리뷰가 숨겨지거나(visible=false) 다시 보이게 됐다. 이미 같은 상태여도 발행한다.
 *
 * @param authorId 작성자 ID. 탈퇴로 연결이 끊겼으면 null
 */
public record PlaceReviewVisibilityChangedEvent(Long reviewId, Long placeId, Long authorId, boolean visible) {}
