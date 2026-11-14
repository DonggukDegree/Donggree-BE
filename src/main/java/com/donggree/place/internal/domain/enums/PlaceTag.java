package com.donggree.place.internal.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ✨ 숨은 맛집 태그. 메인 카테고리와 별개인 추가 범주로, 장소마다 0개 이상 고른다.
 * 강력한 추천일 때만 붙이도록 FE가 안내하며, 영업시간·설명·메뉴처럼 인증 회원 누구나 고친다.
 * 선언 순서가 저장·응답 순서이자 FE 칩·체크박스 순서다.
 */
@Getter
@RequiredArgsConstructor
public enum PlaceTag {
    SHORTCUT("지름길 맛집"),
    STUDY("공부 맛집"),
    GATHERING("회식 맛집"),
    MEAL_DATE("밥약 맛집"),
    MOOD("분위기 맛집"),
    SOLO_DINING("혼밥 맛집"),
    COFFEE("커피 맛집"),
    GENUINE("진짜 맛집");

    private final String label;
}
