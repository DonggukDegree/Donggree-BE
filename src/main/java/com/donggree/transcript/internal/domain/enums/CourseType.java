package com.donggree.transcript.internal.domain.enums;

/**
 * 교과목 이수 구분을 나타내는 열거형.
 * PDF 파싱 결과의 영역 구분에 해당한다.
 */
public enum CourseType {
    COMMON_GENERAL,       // 공통교양
    LIBERAL_ARTS,         // 일반교양
    ACADEMIC_FOUNDATION,  // 학문기초
    FIRST_MAJOR,          // 제1전공
    SECOND_MAJOR          // 제2전공
}
