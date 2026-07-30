package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 졸업 요건 세트 등록/수정 요청. POST(생성)와 PUT(전체 수정)에 공통으로 사용한다.
 * 학과는 단과대명·학과명으로 입력받아, 기존 학과면 재사용하고 없으면 새로 등록한다(find-or-create).
 * 버전은 요청으로 받지 않고 서버가 (학과, 적용년도) 단위로 자동 채번한다.
 * 수정(PUT) 시 학과·단과대는 변경되지 않으며(생성 시 확정), 적용년도가 바뀌면 버전이 새 lineage 기준으로 다시 부여된다.
 * graduationRuleIds는 이 세트에 연결할 졸업 규칙 ID 목록 전체(선택/해제 결과)다.
 */
public record RequirementSetRequest(
        @NotBlank(message = "단과대명은 필수입니다.")
                @Size(max = 100, message = "단과대명은 100자 이하여야 합니다.")
                @Schema(example = "첨단융합대학")
                String collegeName,
        @NotBlank(message = "학과명은 필수입니다.")
                @Size(max = 100, message = "학과명은 100자 이하여야 합니다.")
                @Schema(example = "컴퓨터·AI학부")
                String departmentName,
        @Positive(message = "적용 시작년도는 양수여야 합니다.") @Schema(example = "2023") int yearStart,
        @Positive(message = "적용 종료년도는 양수여야 합니다.") @Schema(example = "2025") int yearEnd,
        @Size(max = 255, message = "설명은 255자 이하여야 합니다.") @Schema(example = "컴퓨터·AI학부 23~25학번 졸업 요건") String description,
        @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.") String sheetImageUrl,
        @Schema(example = "true", description = "활성 여부(미지정 시 true)") Boolean active,
        @Schema(example = "[1, 2, 3]", description = "연결할 졸업 규칙 ID 목록") List<Long> graduationRuleIds) {

    @JsonIgnore
    @AssertTrue(message = "적용 시작년도는 종료년도보다 클 수 없습니다.")
    @Schema(hidden = true)
    public boolean isYearRangeValid() {
        return yearStart <= yearEnd;
    }

    public RequirementSetCommand toCommand() {
        return new RequirementSetCommand(
                collegeName,
                departmentName,
                yearStart,
                yearEnd,
                description,
                sheetImageUrl,
                active == null || active,
                graduationRuleIds == null ? List.of() : graduationRuleIds);
    }
}
