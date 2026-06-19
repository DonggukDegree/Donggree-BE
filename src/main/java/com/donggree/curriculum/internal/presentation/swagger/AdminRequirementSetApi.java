package com.donggree.curriculum.internal.presentation.swagger;

import com.donggree.curriculum.internal.application.dto.RequirementSetResponse;
import com.donggree.curriculum.internal.application.dto.RequirementSetSummaryResponse;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetRequest;
import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Admin-RequirementSet", description = "관리자의 졸업 요건 세트 관리")
public interface AdminRequirementSetApi {

    @Operation(
            summary = "졸업 요건 세트 목록 조회",
            description = "졸업 요건 세트를 동적 필터로 조회한다(요약, 연결 규칙 미포함). "
                    + "departmentId는 해당 학과만, year는 단일 연도로 적용범위(start≤year≤end)에 포함되는 세트만. 미지정=전체.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<RequirementSetSummaryResponse>> getRequirementSets(
            @Parameter(description = "학과 ID 필터") Long departmentId,
            @Parameter(description = "적용 연도 필터(단일)") Integer year);

    @Operation(summary = "졸업 요건 세트 단건 조회", description = "세트 1건과 연결된 졸업 규칙 ID 목록을 조회한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 세트")
    })
    ApiResponse<RequirementSetResponse> getRequirementSet(@Parameter(description = "세트 ID") Long id);

    @Operation(summary = "졸업 요건 세트 생성", description = "새 졸업 요건 세트를 만들고 선택한 졸업 규칙들을 연결한다. 생성된 ID를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류, 존재하지 않는 학과 또는 졸업 규칙"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 학과·적용년도·버전 중복")
    })
    ApiResponse<Long> createRequirementSet(RequirementSetRequest request);

    @Operation(summary = "졸업 요건 세트 수정", description = "세트의 모든 정보(학과·적용년도·버전·설명·이미지·활성)와 연결 규칙을 전체 교체(PUT)한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류, 존재하지 않는 학과 또는 졸업 규칙"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 세트"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 학과·적용년도·버전 중복")
    })
    ApiResponse<Void> updateRequirementSet(@Parameter(description = "세트 ID") Long id, RequirementSetRequest request);
}
