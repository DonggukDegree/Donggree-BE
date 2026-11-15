package com.donggree.point.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.point.internal.presentation.dto.PointHistoryPageResponse;
import com.donggree.point.internal.presentation.dto.PointSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point", description = "내 포인트. 로그인 회원 본인만 조회")
public interface PointApi {

    @Operation(summary = "내 총 포인트", description = "회수되지 않은 적립의 합계를 반환한다.")
    ApiResponse<PointSummaryResponse> getSummary(Long memberId);

    @Operation(
            summary = "내 적립 내역",
            description =
                    "적립 내역을 최신순으로 20개씩 반환한다. 다음 페이지는 nextCursor를 cursor로 넘긴다. " + "숨김으로 회수된 적립도 revoked=true로 포함한다.")
    ApiResponse<PointHistoryPageResponse> getHistories(
            Long memberId, @Parameter(description = "이전 페이지의 nextCursor. 첫 페이지는 생략") Long cursor);
}
