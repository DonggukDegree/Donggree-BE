package com.donggree.place.internal.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ✨ 숨은 맛집 태그. 메인 카테고리와 별개인 추가 범주로, 장소마다 0개 이상 고른다.
 * 강력한 추천일 때만 붙이도록 FE가 안내하며, 영업시간·설명·메뉴처럼 인증 회원 누구나 고친다.
 */
@Getter
@RequiredArgsConstructor
public enum PlaceTag {
    COFFEE("커피 맛집"),
    STUDY("카공 맛집"),
    GATHERING("회식 맛집"),
    MOOD("분위기 맛집"),
    BAR_SNACK("안주 맛집"),
    MEAL_DATE("밥약 맛집"),
    SOLO_DINING("혼밥 맛집");

    private final String label;
}
