package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.command.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.command.GraduationRuleUpsertCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 졸업 규칙 배치 업서트 요청. 등록·수정 항목을 한 번에 담아 전송한다.
 */
public record GraduationRuleBatchRequest(@NotEmpty(message = "items는 비어 있을 수 없습니다.") @Valid List<Item> items) {

    public List<GraduationRuleUpsertCommand> toCommands() {
        return items.stream().map(Item::toCommand).toList();
    }

    /**
     * 배치 업서트의 단일 졸업 규칙 항목.
     * id가 null이면 신규 등록, non-null이면 해당 규칙을 전체 교체(수정)한다.
     * ruleConfig는 규칙 종류별 스키마를 따르는 JSON 객체로 받아 jsonb 문자열로 저장한다.
     * MIN_CREDITS·REQUIRED_COURSE·THESIS·ENGLISH_COURSE의 applicableMajorRoles는 서버에서 필수 여부와 허용값을 검증한다.
     */
    public record Item(
            @Schema(example = "1", description = "수정 대상 규칙 ID. null이면 신규 등록") Long id,
            @NotNull(message = "규칙 종류 ID는 필수입니다.") @Schema(example = "1") Long ruleTypeId,
            @NotBlank(message = "규칙 이름은 필수입니다.")
                    @Size(max = 100, message = "규칙 이름은 100자 이하여야 합니다.")
                    @Schema(example = "총 취득학점이 130학점 이상이어야 합니다.")
                    String ruleName,
            @NotNull(message = "rule_config는 필수입니다.") @Schema(example = "{\"min\":130}", description = "규칙 설정 JSON 객체")
                    JsonNode ruleConfig,
            @Size(max = 255, message = "설명은 255자 이하여야 합니다.") @Schema(example = "총학점 요건") String description) {

        @JsonIgnore
        @AssertTrue(message = "rule_config는 JSON 객체여야 합니다.")
        @Schema(hidden = true)
        public boolean isRuleConfigObject() {
            return ruleConfig != null && ruleConfig.isObject();
        }

        public GraduationRuleUpsertCommand toCommand() {
            return new GraduationRuleUpsertCommand(
                    id, new GraduationRuleCommand(ruleTypeId, ruleName, ruleConfig.toString(), description));
        }
    }
}
