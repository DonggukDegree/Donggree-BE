package com.donggree.curriculum.internal.application.command;

import java.util.List;

/**
 * 졸업 요건 세트 수정 입력 커맨드. 컨트롤러의 수정 요청 DTO를 응용 계층 입력으로 변환한 값이다.
 * 학과·단과대·버전은 생성 시 확정되어 수정 대상이 아니므로 포함하지 않는다.
 * graduationRuleIds는 이 세트에 연결할 졸업 규칙 ID 목록(선택/해제 결과 전체)이다.
 */
public record RequirementSetUpdateCommand(
        int yearStart,
        int yearEnd,
        String description,
        String sheetImageUrl,
        boolean active,
        List<Long> graduationRuleIds) {}
