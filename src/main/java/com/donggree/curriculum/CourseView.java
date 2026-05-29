package com.donggree.curriculum;

/**
 * 다른 모듈에 교과목 카탈로그 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 course_code → course_id 변환에 사용한다.
 */
public record CourseView(
        Long id,
        String courseCode,
        String courseName,
        int credits,
        Long equivalentCourseId
) {}
