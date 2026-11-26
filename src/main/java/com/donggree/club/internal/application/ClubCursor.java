package com.donggree.club.internal.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 목록 정렬 위치. 마지막으로 내려준 동아리의 정렬 키를 담아, 다음 묶음은 이 키보다 뒤인 동아리부터 준다.
 * 클라이언트에는 형식을 감춘 문자열로 내려주고 그대로 돌려받는다.
 *
 * @param group 정렬 구간(값이 없는 동아리를 뒤로 보내는 등)
 * @param value 구간 안의 정렬 값
 */
record ClubCursor(int group, long value, String name, long id) {

    private static final String SEPARATOR = "|";

    String encode() {
        String raw = group + SEPARATOR + value + SEPARATOR + id + SEPARATOR + name;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws IllegalArgumentException 형식이 맞지 않는 커서 */
    static ClubCursor decode(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("커서 형식이 올바르지 않습니다");
        }
        return new ClubCursor(Integer.parseInt(parts[0]), Long.parseLong(parts[1]), parts[3], Long.parseLong(parts[2]));
    }
}
