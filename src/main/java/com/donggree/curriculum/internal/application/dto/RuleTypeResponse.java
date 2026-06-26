package com.donggree.curriculum.internal.application.dto;

import com.donggree.curriculum.CourseType;

/**
 * 규칙 종류 조회 응답. 졸업 규칙 필터의 선택지로 사용된다.
 * courseType이 null이면 졸업요건 규칙(총학점·평점 등), non-null이면 해당 이수구분 영역 규칙이다.
 */
public record RuleTypeResponse(Long id, String typeName, CourseType courseType, String description) {}
