package com.donggree.curriculum.internal.application.dto;

import java.util.List;

/**
 * 졸업 요건 세트 등록/수정 입력 커맨드. 컨트롤러의 요청 DTO를 응용 계층 입력으로 변환한 값이다.
 * 학과는 id가 아니라 단과대명·학과명으로 받아, 응용 계층에서 기존 학과를 재사용하거나 없으면 새로 등록한다.
 * 버전은 클라이언트가 지정하지 않고 응용 계층이 (학과, 적용년도) 단위로 1부터 자동 채번한다.
 * graduationRuleIds는 이 세트에 연결할 졸업 규칙 ID 목록(선택/해제 결과 전체)이다.
 */
public record RequirementSetCommand(
        String collegeName,
        String departmentName,
        int yearStart,
        int yearEnd,
        String description,
        String sheetImageUrl,
        boolean active,
        List<Long> graduationRuleIds) {}
