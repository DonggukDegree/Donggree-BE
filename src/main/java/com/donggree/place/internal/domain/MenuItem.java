package com.donggree.place.internal.domain;

/**
 * 장소 등록·수정 시 입력받는 메뉴 한 줄. 가격이 없으면 "가격 변동"으로 표시한다.
 *
 * @param name  메뉴명
 * @param price 가격(원). null이면 가격 변동
 */
public record MenuItem(String name, Integer price) {

    public static final int NAME_MAX_LENGTH = 30;
    public static final int PRICE_MAX = 1_000_000;

    /** 메뉴명 앞뒤 공백을 지우고 길이·가격 범위를 검증한 값을 반환한다. */
    MenuItem normalize() {
        String validatedName = name == null ? "" : name.trim();
        if (validatedName.isEmpty() || validatedName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("menu name must be 1 to " + NAME_MAX_LENGTH + " characters");
        }
        if (price != null && (price < 0 || price > PRICE_MAX)) {
            throw new IllegalArgumentException("menu price must be between 0 and " + PRICE_MAX);
        }
        return new MenuItem(validatedName, price);
    }
}
