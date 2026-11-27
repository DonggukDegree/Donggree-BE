package com.donggree.club.internal.infrastructure;

import java.util.regex.Pattern;

/** 서버가 만든 사진 저장 이름(UUID.확장자). 이 형식만 받아 경로 이탈·임의 키 접근을 막는다. */
final class ClubImageStoredName {

    private static final Pattern NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private ClubImageStoredName() {}

    static boolean isValid(String storedName) {
        return storedName != null && NAME.matcher(storedName).matches();
    }

    static String require(String storedName) {
        if (!isValid(storedName)) {
            throw new IllegalArgumentException("저장 이름 형식이 올바르지 않습니다");
        }
        return storedName;
    }
}
