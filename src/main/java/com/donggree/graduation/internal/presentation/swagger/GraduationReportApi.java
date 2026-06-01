package com.donggree.graduation.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Graduation", description = "학업 리포트")
public interface GraduationReportApi {

    @Operation(
            summary = "학업 리포트 조회",
            description = "reportId(=성적표 ID)를 기반으로 졸업 요건 판정 결과를 조회한다. " + "전체 달성률·이수 학점 요약과 courseType별 이수 현황을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "성적표 없음 또는 적용 가능한 졸업 요건 없음")
    })
    ApiResponse<GraduationReportResponse> getReport(@Parameter(description = "리포트 ID (= 성적표 ID)") Long reportId);
}
