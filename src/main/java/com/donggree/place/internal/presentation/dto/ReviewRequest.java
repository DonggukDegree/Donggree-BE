package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.command.WriteReviewCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 리뷰 작성·수정 요청. 본문 최소 길이(앞뒤 공백 제외 10자)는 서버 도메인이 다시 검증한다. */
public record ReviewRequest(
        @NotNull(message = "별점은 필수입니다.")
                @Min(value = 1, message = "별점은 1~5점입니다.")
                @Max(value = 5, message = "별점은 1~5점입니다.")
                @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
                Integer rating,
        @NotBlank(message = "리뷰 본문은 필수입니다.")
                @Size(max = 500, message = "리뷰는 500자 이하여야 합니다.")
                @Schema(example = "짜장면이 맛있고 양이 많아요. 점심시간엔 조금 기다려요.", requiredMode = Schema.RequiredMode.REQUIRED)
                String content) {

    public WriteReviewCommand toCommand() {
        return new WriteReviewCommand(rating, content);
    }
}
