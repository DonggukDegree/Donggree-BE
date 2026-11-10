package com.donggree.place.internal.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 동글맵 장소 카테고리. 지도 상단 칩과 핀 아이콘의 기준이며, 변경은 배포로만 한다. */
@Getter
@RequiredArgsConstructor
public enum PlaceCategory {
    RESTAURANT("음식점"),
    CAFE("카페"),
    BAR("술집"),
    STUDY("스터디"),
    LIFE("생활"),
    ETC("기타");

    private final String label;
}
