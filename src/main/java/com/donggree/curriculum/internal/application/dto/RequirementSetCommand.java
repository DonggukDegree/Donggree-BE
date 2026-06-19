package com.donggree.curriculum.internal.application.dto;

import java.util.List;

/**
 * 졸업 요건 세트 등록/수정 입력 커맨드. 컨트롤러의 요청 DTO를 응용 계층 입력으로 변환한 값이다.
 * graduationRuleIds는 이 세트에 연결할 졸업 규칙 ID 목록(선택/해제 결과 전체)이다.
 */
public record RequirementSetCommand(
        Long departmentId,
        int yearStart,
        int yearEnd,
        int version,
        String description,
        String sheetImageUrl,
        boolean active,
        List<Long> graduationRuleIds) {}
