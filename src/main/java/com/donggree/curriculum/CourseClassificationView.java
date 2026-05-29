package com.donggree.curriculum;

/**
 * 다른 모듈에 과목 분류 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 course_code → 졸업 판정용 분류(courseType, areaTypeId, subCategory, subjectDomain) 매핑에 사용한다.
 */
public record CourseClassificationView(
        Long courseId,
        CourseType courseType,
        Long areaTypeId,
        String subCategory,
        String subjectDomain
) {}
