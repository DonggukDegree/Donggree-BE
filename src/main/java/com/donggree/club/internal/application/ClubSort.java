package com.donggree.club.internal.application;

/** 목록 정렬. 같은 값끼리는 이름순, 그다음 ID순이다. */
public enum ClubSort {
    /** 이름순(기본) */
    NAME,
    /** 마감 임박순: 기간 모집 중(마감 빠른 순) → 상시모집 → 나머지 */
    DEADLINE,
    /** 전체 활동 인원 많은 순. 인원 미입력은 맨 뒤 */
    MEMBERS,
    /** 창립일 오래된 순. 창립일 모름은 맨 뒤 */
    OLDEST
}
