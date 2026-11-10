package com.donggree.place.internal.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 동글맵 상세카테고리. 각 값은 하나의 상위 카테고리에만 속한다. */
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

    COFFEE(PlaceCategory.CAFE, "커피"),
    DESSERT(PlaceCategory.CAFE, "디저트/베이커리"),
    CAFE_ETC(PlaceCategory.CAFE, "기타"),

    PUB(PlaceCategory.BAR, "호프/요리주점"),
    IZAKAYA(PlaceCategory.BAR, "이자카야"),
    POCHA(PlaceCategory.BAR, "포차"),
    WINE_BAR(PlaceCategory.BAR, "바/와인"),
    BAR_ETC(PlaceCategory.BAR, "기타"),

    STUDY_CAFE(PlaceCategory.STUDY, "스터디카페"),
    STUDY_FRIENDLY_CAFE(PlaceCategory.STUDY, "카공 카페"),
    CAMPUS_FACILITY(PlaceCategory.STUDY, "학교 시설"),
    STUDY_ETC(PlaceCategory.STUDY, "기타"),

    CONVENIENCE_STORE(PlaceCategory.LIFE, "편의점"),
    PRINT(PlaceCategory.LIFE, "인쇄/복사"),
    STATIONERY(PlaceCategory.LIFE, "문구"),
    MEDICAL(PlaceCategory.LIFE, "병원/약국"),
    BEAUTY(PlaceCategory.LIFE, "미용"),
    LIFE_ETC(PlaceCategory.LIFE, "기타"),

    ETC(PlaceCategory.ETC, "기타");

    private final PlaceCategory category;
    private final String label;

    public boolean belongsTo(PlaceCategory category) {
        return this.category == category;
    }
}
