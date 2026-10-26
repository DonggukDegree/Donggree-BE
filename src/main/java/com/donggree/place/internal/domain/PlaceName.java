package com.donggree.place.internal.domain;

import java.util.Locale;

/** 중복 장소 비교용 이름 정규화. 공백·특수문자를 제거하고 영문은 소문자로 맞춘다. */
public final class PlaceName {

    private PlaceName() {}

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(name.length());
        name.codePoints().filter(Character::isLetterOrDigit).forEach(normalized::appendCodePoint);
        return normalized.toString().toLowerCase(Locale.ROOT);
    }
}
