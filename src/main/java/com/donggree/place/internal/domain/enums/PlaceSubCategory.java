package com.donggree.place.internal.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 동글맵 상세카테고리. 각 값은 하나의 상위 카테고리에만 속한다.
 * 2026-11-12 개편으로 포차(POCHA)·카공 카페(STUDY_FRIENDLY_CAFE)를 없앴고, 기존 데이터는 V7에서 각 카테고리의 기타로 옮긴다.
 */
@Getter
@RequiredArgsConstructor
public enum PlaceSubCategory {
    KOREAN(PlaceCategory.RESTAURANT, "한식"),
    CHINESE(PlaceCategory.RESTAURANT, "중식"),
    JAPANESE(PlaceCategory.RESTAURANT, "일식"),
    WESTERN(PlaceCategory.RESTAURANT, "양식"),
    SNACK(PlaceCategory.RESTAURANT, "분식"),
    ASIAN(PlaceCategory.RESTAURANT, "아시안"),
    MEAT(PlaceCategory.RESTAURANT, "고기/구이"),
    FAST_FOOD(PlaceCategory.RESTAURANT, "패스트푸드"),
    RESTAURANT_ETC(PlaceCategory.RESTAURANT, "기타"),
    FRANCHISE(PlaceCategory.CAFE, "프랜차이즈"),
    DESSERT(PlaceCategory.CAFE, "디저트/베이커리"),
    COFFEE(PlaceCategory.CAFE, "커피 전문"),
    CAFE_ETC(PlaceCategory.CAFE, "기타"),
    PUB(PlaceCategory.BAR, "호프/요리주점"),
    IZAKAYA(PlaceCategory.BAR, "이자카야"),
    WINE_BAR(PlaceCategory.BAR, "바/와인"),
    BAR_ETC(PlaceCategory.BAR, "기타"),
    STUDY_CAFE(PlaceCategory.STUDY, "스터디카페/독서실"),
    CAMPUS_FACILITY(PlaceCategory.STUDY, "학교 시설"),
    LIBRARY(PlaceCategory.STUDY, "도서관"),
    STUDY_ETC(PlaceCategory.STUDY, "기타"),
    CONVENIENCE_STORE(PlaceCategory.LIFE, "편의점"),
    PRINT(PlaceCategory.LIFE, "인쇄/복사"),
    STATIONERY(PlaceCategory.LIFE, "문구"),
    MEDICAL(PlaceCategory.LIFE, "병원/약국"),
    BEAUTY(PlaceCategory.LIFE, "미용"),
    MART(PlaceCategory.LIFE, "마트"),
    FITNESS(PlaceCategory.LIFE, "헬스/운동"),
    LIFE_ETC(PlaceCategory.LIFE, "기타"),
    BILLIARDS(PlaceCategory.ENTERTAINMENT, "당구장"),
    BOWLING(PlaceCategory.ENTERTAINMENT, "볼링장"),
    KARAOKE(PlaceCategory.ENTERTAINMENT, "노래방"),
    PC_ROOM(PlaceCategory.ENTERTAINMENT, "PC방"),
    ENTERTAINMENT_ETC(PlaceCategory.ENTERTAINMENT, "기타"),
    ETC(PlaceCategory.ETC, "기타");

    private final PlaceCategory category;
    private final String label;

    public boolean belongsTo(PlaceCategory category) {
        return this.category == category;
    }
}
