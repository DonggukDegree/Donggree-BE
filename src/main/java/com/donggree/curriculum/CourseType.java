package com.donggree.curriculum;

/**
 * 졸업 판정에 사용하는 이수 구분 enum.
 * course_record의 course_type_name(PDF 원시값)과 다르며,
 * course_classification 테이블에 저장되는 정제된 값이다.
 * graduation 모듈에서도 참조하므로 public 패키지에 둔다.
 */
public enum CourseType {
    COMMON_GENERAL,
    LIBERAL_ARTS,
    ACADEMIC_FOUNDATION,
    FIRST_MAJOR,
    SECOND_MAJOR
}
