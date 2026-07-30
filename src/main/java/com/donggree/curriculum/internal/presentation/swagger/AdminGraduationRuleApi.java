package com.donggree.curriculum.internal.presentation.swagger;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.presentation.dto.GraduationRuleBatchRequest;
import com.donggree.curriculum.internal.presentation.dto.GraduationRuleResponse;
import com.donggree.curriculum.internal.presentation.dto.RuleTypeResponse;
import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Admin-GraduationRule", description = "관리자의 졸업 규칙·규칙 종류 관리")
public interface AdminGraduationRuleApi {

    @Operation(summary = "규칙 종류 조회", description = "규칙 종류(rule_type) 전체를 조회한다. 졸업 규칙 필터의 선택지로 사용된다. 읽기 전용.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<RuleTypeResponse>> getRuleTypes();

    @Operation(
            summary = "졸업 규칙 조회",
            description = "졸업 규칙을 동적 다중 필터로 조회한다. ruleTypeIds·courseTypes는 하나라도 일치(IN), 미지정=전체. "
                    + "courseType은 rule_type 소유 컬럼이며, 정렬은 course_type(NULLS LAST) 그다음 rule_type_id 순이다. "
                    + "응답에 rule_type 정보(typeName, courseType)와 ruleConfig(JSON 객체)가 포함된다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<GraduationRuleResponse>> getGraduationRules(
            @Parameter(description = "규칙 종류 ID 필터(다중)") List<Long> ruleTypeIds,
            @Parameter(description = "이수구분 필터(다중)") List<CourseType> courseTypes);

    @Operation(
            summary = "졸업 규칙 배치 업서트",
            description = "등록·수정을 구분하지 않고 여러 졸업 규칙을 한 번에 변경한다. "
                    + "각 항목의 id가 null이면 신규 등록, non-null이면 전체 교체(수정)다. "
                    + "전체가 하나의 트랜잭션으로 처리되어 일부라도 실패하면 모두 롤백된다. 결과 ID 목록을 입력 순서대로 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업서트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류 또는 존재하지 않는 규칙 종류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 졸업 규칙(id 지정)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "동일 규칙종류·규칙이름 중복(요청 내 또는 기존과)")
    })
    ApiResponse<List<Long>> upsertGraduationRules(GraduationRuleBatchRequest request);
}
