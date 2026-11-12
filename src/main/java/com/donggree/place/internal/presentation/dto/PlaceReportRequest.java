package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.command.ReportPlaceCommand;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 장소 신고 요청. 기타(OTHER) 사유의 설명 필수 여부는 서버가 다시 검증한다. */
public record PlaceReportRequest(
        @NotNull(message = "신고 사유는 필수입니다.") @Schema(example = "CLOSED", requiredMode = Schema.RequiredMode.REQUIRED)
                PlaceReportReason reason,
        @Size(max = 200, message = "설명은 200자 이하여야 합니다.") @Schema(example = "10월부터 영업하지 않아요", description = "기타 사유는 필수")
                String description) {

    public ReportPlaceCommand toCommand() {
        return new ReportPlaceCommand(reason, description);
    }
}
