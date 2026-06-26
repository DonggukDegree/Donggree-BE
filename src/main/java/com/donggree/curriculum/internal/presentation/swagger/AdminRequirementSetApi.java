package com.donggree.curriculum.internal.presentation.swagger;

import com.donggree.curriculum.internal.application.dto.RequirementSetResponse;
import com.donggree.curriculum.internal.application.dto.RequirementSetSummaryResponse;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetRequest;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetUpdateRequest;
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
                    + "departmentId는 특정 학과, collegeId는 해당 단과대에 속한 모든 학과의 세트를(둘 다 주면 교집합), "
                    + "year는 단일 연도로 적용범위(start≤year≤end)에 포함되는 세트만. 미지정=전체.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<RequirementSetSummaryResponse>> getRequirementSets(
            @Parameter(description = "학과 ID 필터(드롭다운에서 선택)") Long departmentId,
            @Parameter(description = "단과대 ID 필터(드롭다운에서 선택)") Long collegeId,
            @Parameter(description = "적용 연도 필터(단일)") Integer year);

    @Operation(summary = "졸업 요건 세트 단건 조회", description = "세트 1건과 연결된 졸업 규칙 ID 목록을 조회한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 세트")
    })
    ApiResponse<RequirementSetResponse> getRequirementSet(@Parameter(description = "세트 ID") Long id);

    @Operation(
            summary = "졸업 요건 세트 생성",
            description =
                    "새 졸업 요건 세트를 만들고 선택한 졸업 규칙들을 연결한다. 학과는 단과대명·학과명으로 받아 기존 학과면 재사용, 없으면 새로 등록한다. "
                            + "버전은 요청으로 받지 않고 서버가 (학과, 적용년도) 단위로 1부터 자동 채번한다. 활성으로 등록하면 같은 학과에 적용년도가 겹치는 다른 활성 세트가 없어야 한다. 생성된 ID를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류 또는 존재하지 않는 졸업 규칙"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "적용년도가 겹치는 다른 활성 세트 존재")
    })
    ApiResponse<Long> createRequirementSet(RequirementSetRequest request);

    @Operation(
            summary = "졸업 요건 세트 수정",
            description =
                    "세트의 정보(적용년도·설명·이미지·활성)와 연결 규칙을 전체 교체(PUT)한다. "
                            + "학과·단과대·버전은 생성 시 확정되어 수정할 수 없으므로 요청에 포함되지 않는다. "
                            + "적용년도가 바뀌면 버전을 새 (학과, 적용년도) 기준으로 다시 부여하고, 그대로면 기존 버전을 유지한다. 활성으로 둘 경우 적용년도가 겹치는 다른 활성 세트가 없어야 한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류 또는 존재하지 않는 졸업 규칙"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 세트"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "적용년도가 겹치는 다른 활성 세트 존재")
    })
    ApiResponse<Void> updateRequirementSet(
            @Parameter(description = "세트 ID") Long id, RequirementSetUpdateRequest request);
}
