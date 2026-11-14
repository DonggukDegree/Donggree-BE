package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.command.ReportReviewCommand;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 리뷰 신고 요청. 기타(OTHER) 사유의 설명 필수 여부는 서버가 다시 검증한다. */
public record ReviewReportRequest(
        @NotNull(message = "신고 사유는 필수입니다.") @Schema(example = "ABUSE", requiredMode = Schema.RequiredMode.REQUIRED)
                ReviewReportReason reason,
        @Size(max = 200, message = "설명은 200자 이하여야 합니다.") @Schema(description = "기타 사유는 필수") String description) {

    public ReportReviewCommand toCommand() {
        return new ReportReviewCommand(reason, description);
    }
}
