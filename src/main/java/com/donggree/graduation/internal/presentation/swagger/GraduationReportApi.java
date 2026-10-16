package com.donggree.graduation.internal.presentation.swagger;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Graduation", description = "학업 리포트")
public interface GraduationReportApi {

    @Operation(
            summary = "학업 리포트 조회",
            description = "로그인한 회원의 졸업 요건 판정 결과를 조회한다. " + "전체 달성률·이수 학점 요약과 courseType별 이수 현황을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "성적표 없음 또는 적용 가능한 졸업 요건 없음")
    })
    ApiResponse<GraduationReportResponse> getReport(@Parameter(hidden = true) Long memberId);

    @Operation(
            summary = "영역별 이수 현황 조회",
            description =
                    "courseType 내 areaType별 상세 이수 현황을 조회한다. " + "areaType별 이수 과목 목록(필수/선택), 미충족 사유, 전체 학점 현황을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "성적표 없음 또는 적용 가능한 졸업 요건 없음")
    })
    ApiResponse<AreaDetailResponse> getAreaDetail(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "이수 구분 코드 (COMMON_GENERAL, ACADEMIC_FOUNDATION, FIRST_MAJOR 등)")
                    CourseType courseType);
}
