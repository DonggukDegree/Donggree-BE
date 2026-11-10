package com.donggree.curriculum;

import java.util.Arrays;

/** 저장 요청에서 허용하는 규칙 종류. graduation 판정기와의 일치는 통합 테스트로 검증한다. */
public enum GraduationRuleType {
    TOTAL_CREDITS,
    GPA,
    MIN_CREDITS,
    REQUIRED_COURSE,
    PREREQUISITE,
    SCIENCE_CONFLICT,
    THESIS,
    ENGLISH_COURSE,
    TEACHING_APTITUDE;

    public static boolean supports(String typeName) {
        return Arrays.stream(values()).anyMatch(type -> type.name().equals(typeName));
    }
}
