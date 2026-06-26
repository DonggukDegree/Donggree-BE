package com.donggree.curriculum.internal.application.dto;

import com.donggree.curriculum.CourseType;

/**
 * 과목 분류 등록/수정 입력 커맨드. 컨트롤러의 요청 DTO를 응용 계층 입력으로 변환한 값이다.
 * tag는 표시용 자유 텍스트로 검증하지 않는다.
 */
public record CourseClassificationCommand(
        String courseCode,
        String tag,
        int studentYearStart,
        int studentYearEnd,
        CourseType courseType,
        Long areaTypeId,
        String subCategory,
        String subjectDomain) {}
