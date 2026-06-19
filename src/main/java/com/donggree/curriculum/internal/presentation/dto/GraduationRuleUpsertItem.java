package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.dto.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.dto.GraduationRuleUpsertCommand;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 배치 업서트의 단일 졸업 규칙 항목.
 * id가 null이면 신규 등록, non-null이면 해당 규칙을 전체 교체(수정)한다.
 * ruleConfig는 규칙 종류별 스키마를 따르는 JSON 객체로 받아 jsonb 문자열로 저장한다(서버는 스키마 검증하지 않음).
 */
public record GraduationRuleUpsertItem(
        @Schema(example = "1", description = "수정 대상 규칙 ID. null이면 신규 등록") Long id,
        @NotNull(message = "규칙 종류 ID는 필수입니다.") @Schema(example = "1") Long ruleTypeId,
        @NotBlank(message = "규칙 이름은 필수입니다.")
                @Size(max = 100, message = "규칙 이름은 100자 이하여야 합니다.")
                @Schema(example = "총 취득학점이 130학점 이상이어야 합니다.")
                String ruleName,
        @NotNull(message = "rule_config는 필수입니다.") @Schema(example = "{\"min\":130}", description = "규칙 설정 JSON 객체")
                JsonNode ruleConfig,
        @Size(max = 255, message = "설명은 255자 이하여야 합니다.") @Schema(example = "총학점 요건") String description) {

    public GraduationRuleUpsertCommand toCommand() {
        return new GraduationRuleUpsertCommand(
                id, new GraduationRuleCommand(ruleTypeId, ruleName, ruleConfig.toString(), description));
    }
}
