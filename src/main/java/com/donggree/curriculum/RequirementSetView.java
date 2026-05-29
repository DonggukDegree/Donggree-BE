package com.donggree.curriculum;

/**
 * 다른 모듈에 졸업 요건 세트 메타 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 학과 + 입학년도에 맞는 requirement_set을 조회할 때 사용한다.
 */
public record RequirementSetView(
        Long id,
        Long departmentId,
        int yearStart,
        int yearEnd
) {}
