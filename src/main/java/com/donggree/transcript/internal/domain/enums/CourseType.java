package com.donggree.transcript.internal.domain.enums;

/**
 * 교과목 이수 구분을 나타내는 열거형.
 * PDF 파싱 결과의 영역 구분에 해당한다.
 */
public enum CourseType {
    COMMON_GENERAL,       // 공통교양 (공교)
    LIBERAL_ARTS,         // 일반교양 (일교)
    ACADEMIC_FOUNDATION,  // 학문기초 (학기)
    FIRST_MAJOR,          // 제1전공 (전공, 전필)
    SECOND_MAJOR,         // 제2전공
    FREE_ELECTIVE;        // 자유선택 (자선)

    /**
     * PDF 파싱 카테고리 문자열을 CourseType으로 변환한다.
     *
     * @param category PDF 이수구분 (공교, 전공, 전필, 일교, 학기, 자선)
     * @return 대응하는 CourseType
     * @throws IllegalArgumentException 알 수 없는 카테고리인 경우
     */
    public static CourseType fromCategory(String category) {
        return switch (category) {
            case "공교" -> COMMON_GENERAL;
            case "일교" -> LIBERAL_ARTS;
            case "학기" -> ACADEMIC_FOUNDATION;
            case "전공", "전필" -> FIRST_MAJOR;
            case "자선" -> FREE_ELECTIVE;
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
    }
}
