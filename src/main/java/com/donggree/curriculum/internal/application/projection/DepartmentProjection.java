package com.donggree.curriculum.internal.application.projection;

/**
 * 학과 조회 응답. 필터·등록 드롭다운의 선택지로 사용된다.
 * 사용자는 이름(collegeName·departmentName)을 보고 고르고, 프론트는 id(departmentId·collegeId)로 요청/응답한다.
 */
public record DepartmentProjection(Long id, Long collegeId, String collegeName, String departmentName) {}
