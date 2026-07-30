package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.command.CourseClassificationCommand;
import com.donggree.curriculum.internal.application.command.CourseClassificationUpsertCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 과목 분류 배치 업서트 요청. 등록·수정 항목을 한 번에 담아 전송한다.
 */
public record CourseClassificationBatchRequest(@NotEmpty(message = "items는 비어 있을 수 없습니다.") @Valid List<Item> items) {

    public List<CourseClassificationUpsertCommand> toCommands() {
        return items.stream().map(Item::toCommand).toList();
    }

    /**
     * 배치 업서트의 단일 과목 분류 항목.
     * id가 null이면 신규 등록, non-null이면 해당 분류를 전체 교체(수정)한다.
     * tag는 표시용 자유 텍스트로 검증하지 않는다(선택값).
     */
    public record Item(
            @Schema(example = "1", description = "수정 대상 분류 ID. null이면 신규 등록") Long id,
            @NotBlank(message = "과목코드는 필수입니다.")
                    @Size(max = 20, message = "과목코드는 20자 이하여야 합니다.")
                    @Schema(example = "CSE2001")
                    String courseCode,
            @Size(max = 100, message = "tag는 100자 이하여야 합니다.")
                    @Schema(example = "자료구조", description = "과목명 등 표시용 자유 텍스트")
                    String tag,
            @Positive(message = "적용 시작년도는 양수여야 합니다.") @Schema(example = "2023") int studentYearStart,
            @Positive(message = "적용 종료년도는 양수여야 합니다.") @Schema(example = "2025") int studentYearEnd,
            @NotNull(message = "이수구분은 필수입니다.") @Schema(example = "FIRST_MAJOR") CourseType courseType,
            @Schema(example = "10", description = "이수 영역 ID (없으면 null)") Long areaTypeId,
            @Size(max = 20, message = "소분류는 20자 이하여야 합니다.") @Schema(example = "개론") String subCategory,
            @Size(max = 20, message = "세부도메인은 20자 이하여야 합니다.") @Schema(example = "물리") String subjectDomain) {

        @JsonIgnore
        @AssertTrue(message = "적용 시작년도는 종료년도보다 클 수 없습니다.")
        @Schema(hidden = true)
        public boolean isYearRangeValid() {
            return studentYearStart <= studentYearEnd;
        }

        public CourseClassificationUpsertCommand toCommand() {
            return new CourseClassificationUpsertCommand(
                    id,
                    new CourseClassificationCommand(
                            courseCode,
                            tag,
                            studentYearStart,
                            studentYearEnd,
                            courseType,
                            areaTypeId,
                            subCategory,
                            subjectDomain));
        }
    }
}
