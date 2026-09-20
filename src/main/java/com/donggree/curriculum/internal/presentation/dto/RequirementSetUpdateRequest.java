package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.command.RequirementSetUpdateCommand;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 졸업 요건 세트 수정(PUT) 요청. 생성 요청과 달리 학과·단과대·버전은 받지 않는다(생성 시 확정, 변경 불가).
 * 적용년도가 바뀌면 서버가 버전을 새 (학과, 적용년도) 기준으로 다시 부여한다.
 * track은 이 세트를 적용할 과정 구분이다. 과정 차이가 없는 대부분의 학과는 ALL 하나만 등록한다.
 * 공학인증을 운영해 일반/심화 요건이 다른 학과만 GENERAL·ADVANCED 세트를 각각 등록한다.
 * graduationRuleIds는 이 세트에 연결할 졸업 규칙 ID 목록 전체(선택/해제 결과)다.
 */
public record RequirementSetUpdateRequest(
        @Positive(message = "적용 시작년도는 양수여야 합니다.") @Schema(example = "2023") int yearStart,
        @Positive(message = "적용 종료년도는 양수여야 합니다.") @Schema(example = "2025") int yearEnd,
        @Schema(example = "ALL", description = "적용 과정(ALL=과정 구분 없음, GENERAL=일반과정, ADVANCED=심화과정). 미지정 시 ALL")
                RequirementTrack track,
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

    public RequirementSetUpdateCommand toCommand() {
        return new RequirementSetUpdateCommand(
                yearStart,
                yearEnd,
                track == null ? RequirementTrack.ALL : track,
                description,
                sheetImageUrl,
                active == null || active,
                graduationRuleIds == null ? List.of() : graduationRuleIds);
    }
}
