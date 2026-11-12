package com.donggree.place.internal.domain.enums;

/**
 * 장소 신고 처리 상태. 대기(PENDING) → 처리 완료(RESOLVED: 숨김·관리자 수정) 또는 기각(REJECTED).
 * 자동 숨김과 신고자 결과 알림은 없다.
 */
public enum PlaceReportStatus {
    PENDING,
    RESOLVED,
    REJECTED
}
