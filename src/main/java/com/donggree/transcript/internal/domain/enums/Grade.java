package com.donggree.transcript.internal.domain.enums;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 성적 등급을 나타내는 열거형.
 * Java enum 이름에 '+' 문자를 사용할 수 없으므로 DB 값 매핑용 value 필드를 별도로 둔다.
 * 예: A_PLUS("A+"), A_ZERO("A0")
 *
 * gradePoint는 평점 평균(GPA) 계산에 사용하는 등급별 평점이다.
 * A+ 4.5부터 0.5씩 차감하여 D0 1.0까지, F·P·NP는 0.0이다.
 * (P·NP는 평점은 0이지만 학점 수에는 포함된다 — GPA 계산 규칙 참고)
 */
public enum Grade {
    A_PLUS("A+", "4.5"),
    A_ZERO("A0", "4.0"),
    B_PLUS("B+", "3.5"),
    B_ZERO("B0", "3.0"),
    C_PLUS("C+", "2.5"),
    C_ZERO("C0", "2.0"),
    D_PLUS("D+", "1.5"),
    D_ZERO("D0", "1.0"),
    F("F", "0.0"),
    P("P", "0.0"),
    NP("NP", "0.0");

    private final String value;
    private final BigDecimal gradePoint;

    private static final Map<String, Grade> VALUE_MAP =
            Stream.of(values()).collect(Collectors.toMap(Grade::getValue, g -> g));

    Grade(String value, String gradePoint) {
        this.value = value;
        this.gradePoint = new BigDecimal(gradePoint);
    }

    public String getValue() {
        return value;
    }

    /**
     * GPA 계산용 등급별 평점을 반환한다. (A+=4.5 ... D0=1.0, F·P·NP=0.0)
     */
    public BigDecimal getGradePoint() {
        return gradePoint;
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
