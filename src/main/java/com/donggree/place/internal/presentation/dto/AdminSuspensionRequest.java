package com.donggree.place.internal.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 관리자 정지 요청. 정지 일수는 1일 이상 정수(업무 상한 없음). memberId는 장소 신고로 정지할 때만 쓴다
 * (등록자·수정자 중 대상 회원). 리뷰 신고는 리뷰 작성자, 회원 화면은 경로의 회원을 정지한다.
 */
public record AdminSuspensionRequest(
        @Schema(description = "장소 신고로 정지할 대상 회원 ID(장소 신고에서만 필수)") Long memberId,
        @NotNull(message = "정지 일수는 필수입니다.")
                @Min(value = 1, message = "정지 일수는 1일 이상이어야 합니다.")
                @Schema(example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
                Integer days,
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.") @Schema(description = "관리자 메모(선택)") String note) {}
