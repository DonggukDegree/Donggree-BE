package com.donggree.transcript.internal.domain.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 성적 등급을 나타내는 열거형.
 * Java enum 이름에 '+' 문자를 사용할 수 없으므로 DB 값 매핑용 value 필드를 별도로 둔다.
 * 예: A_PLUS("A+"), A_ZERO("A0")
 */
public enum Grade {
    A_PLUS("A+"),
    A_ZERO("A0"),
    B_PLUS("B+"),
    B_ZERO("B0"),
    C_PLUS("C+"),
    C_ZERO("C0"),
    D_PLUS("D+"),
    D_ZERO("D0"),
    F("F"),
    P("P");

    private final String value;

    private static final Map<String, Grade> VALUE_MAP =
            Stream.of(values()).collect(Collectors.toMap(Grade::getValue, g -> g));

    Grade(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * DB에 저장된 문자열 값으로부터 Grade enum을 찾는다.
     * 매칭되는 값이 없으면 IllegalArgumentException을 던진다.
     */
    public static Grade fromValue(String value) {
        Grade grade = VALUE_MAP.get(value);
        if (grade == null) {
            throw new IllegalArgumentException("Unknown grade value: " + value);
        }
        return grade;
    }
}
