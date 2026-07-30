package com.donggree.curriculum.internal.presentation.swagger;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.presentation.dto.AreaTypeResponse;
import com.donggree.curriculum.internal.presentation.dto.CourseClassificationBatchRequest;
import com.donggree.curriculum.internal.presentation.dto.CourseClassificationResponse;
import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Admin-Curriculum", description = "관리자의 커리큘럼(과목 분류·이수 영역) 관리")
public interface AdminCurriculumApi {

    @Operation(
            summary = "과목 분류 조회",
            description = "과목 분류를 동적 다중 필터로 조회한다. 각 필터는 여러 값을 지정할 수 있고, 지정하지 않으면 전체 조회다. "
                    + "areaTypeIds·courseTypes는 하나라도 일치(IN), years는 지정 연도 중 하나라도 적용 입학년도 범위(start≤year≤end)에 포함되면 매칭(OR). "
                    + "응답에는 area_type_id 대신 영역 이름(areaName)과 과목 tag가 포함된다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<CourseClassificationResponse>> getCourseClassifications(
            @Parameter(description = "이수 영역 ID 필터(다중)") List<Long> areaTypeIds,
            @Parameter(description = "이수구분 필터(다중)") List<CourseType> courseTypes,
            @Parameter(description = "적용 입학년도 필터(다중)") List<Integer> years);

    @Operation(
            summary = "과목 분류 배치 업서트",
            description = "등록·수정을 구분하지 않고 여러 과목 분류를 한 번에 변경한다. "
                    + "각 항목의 id가 null이면 신규 등록, non-null이면 전체 교체(수정)다. "
                    + "전체가 하나의 트랜잭션으로 처리되어 일부라도 실패하면 모두 롤백된다. 결과 ID 목록을 입력 순서대로 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업서트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류 또는 존재하지 않는 이수 영역"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 과목 분류(id 지정)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "동일 과목코드·적용년도 범위 중복(요청 내 또는 기존과)")
    })
    ApiResponse<List<Long>> upsertCourseClassifications(CourseClassificationBatchRequest request);

    @Operation(summary = "이수 영역 조회", description = "이수 영역 전체를 조회한다. 과목 분류 필터의 선택지로 사용된다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<AreaTypeResponse>> getAreaTypes();
}
