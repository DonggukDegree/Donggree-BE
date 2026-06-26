package com.donggree.curriculum;

/**
 * 다른 모듈에 과목 분류 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * 4계층 구조: courseType(이수구분) > areaName(영역) > subCategory(소분류) > subjectDomain(세부도메인)
 * course_classification에 등록된 과목은 이 데이터를 사용하고,
 * 미등록 과목은 PDF course_type_name으로 courseType만 추론한 뒤 나머지는 null이다.
 */
public record CourseClassificationView(
        CourseType courseType, String areaName, String subCategory, String subjectDomain) {}
