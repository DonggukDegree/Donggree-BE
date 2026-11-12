package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Review", description = "관리자 리뷰 숨김·숨김 해제. ADMIN 이상")
public interface AdminReviewApi {

    @Operation(summary = "리뷰 숨김", description = "리뷰를 목록·평균에서 빼고 그 리뷰의 처리 대기 신고를 처리 완료로 바꾼다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "숨김 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 삭제된 리뷰")
    })
    ApiResponse<Void> hideReview(@Parameter(description = "리뷰 ID") Long reviewId);

    @Operation(summary = "리뷰 숨김 해제", description = "리뷰를 다시 보이게 하고 그 리뷰의 처리 대기 신고를 기각한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "숨김 해제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 삭제된 리뷰")
    })
    ApiResponse<Void> unhideReview(@Parameter(description = "리뷰 ID") Long reviewId);
}
