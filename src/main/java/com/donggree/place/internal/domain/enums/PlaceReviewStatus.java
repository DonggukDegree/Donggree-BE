package com.donggree.place.internal.domain.enums;

/** 리뷰 노출 상태. 신고 누적 자동 숨김이나 관리자 숨김이면 HIDDEN. 작성자 삭제는 별도(삭제 시각)로 남긴다. */
public enum PlaceReviewStatus {
    VISIBLE,
    HIDDEN
}
