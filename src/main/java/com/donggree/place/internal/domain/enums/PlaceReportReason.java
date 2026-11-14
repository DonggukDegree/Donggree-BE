package com.donggree.place.internal.domain.enums;

/** 장소 신고 사유. 기타(OTHER)는 설명이 필수다. */
public enum PlaceReportReason {
    CLOSED,
    WRONG_LOCATION,
    WRONG_NAME,
    DUPLICATE,
    INAPPROPRIATE,
    OTHER
}
