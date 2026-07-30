package com.donggree.curriculum.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.internal.application.RequirementSetCommandService;
import com.donggree.curriculum.internal.application.RequirementSetQueryService;
import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.donggree.curriculum.internal.application.projection.RequirementSetProjection;
import com.donggree.curriculum.internal.application.projection.RequirementSetSummaryProjection;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetRequest;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetUpdateRequest;
import com.donggree.global.support.RestDocsSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

class AdminRequirementSetControllerTest extends RestDocsSupport {

    private final RequirementSetQueryService requirementSetQueryService =
            Mockito.mock(RequirementSetQueryService.class);
    private final RequirementSetCommandService requirementSetCommandService =
            Mockito.mock(RequirementSetCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new AdminRequirementSetController(requirementSetQueryService, requirementSetCommandService);
    }

    private RequirementSetProjection sample() {
        return new RequirementSetProjection(
                1L, 10L, "컴퓨터·AI학부", 2023, 2025, 1, "23~25학번 졸업 요건", "https://img/sheet.png", true, List.of(1L, 2L));
    }

    private RequirementSetSummaryProjection summarySample() {
        return new RequirementSetSummaryProjection(
                1L, 10L, "컴퓨터·AI학부", 2023, 2025, 1, "23~25학번 졸업 요건", "https://img/sheet.png", true);
    }

    @Test
    void 졸업_요건_세트_목록을_조회한다() throws Exception {
        given(requirementSetQueryService.search(10L, 2L, 2024)).willReturn(List.of(summarySample()));

        mockMvc.perform(get("/api/admin/requirement-sets")
                        .param("departmentId", "10")
                        .param("collegeId", "2")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].departmentName").value("컴퓨터·AI학부"))
                .andDo(document(
                        "admin-requirement-set-list",
                        queryParameters(
                                parameterWithName("departmentId").optional().description("학과 ID 필터(드롭다운 선택, 미지정=전체)"),
                                parameterWithName("collegeId").optional().description("단과대 ID 필터(드롭다운 선택, 미지정=전체)"),
                                parameterWithName("year").optional().description("적용 연도 필터(단일, start≤year≤end)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("세트 ID"),
                                fieldWithPath("result[].departmentId").description("학과 ID"),
                                fieldWithPath("result[].departmentName")
                                        .optional()
                                        .description("학과명 (없으면 null)"),
                                fieldWithPath("result[].yearStart").description("적용 시작년도"),
                                fieldWithPath("result[].yearEnd").description("적용 종료년도"),
                                fieldWithPath("result[].version").description("버전"),
                                fieldWithPath("result[].description").optional().description("설명 (없으면 null)"),
                                fieldWithPath("result[].sheetImageUrl")
                                        .optional()
                                        .description("시트 이미지 URL (없으면 null)"),
                                fieldWithPath("result[].active").description("활성 여부"))));
    }

    @Test
    void 졸업_요건_세트_단건을_조회한다() throws Exception {
        given(requirementSetQueryService.get(1L)).willReturn(sample());

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/requirement-sets/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.graduationRuleIds[1]").value(2))
                .andDo(document(
                        "admin-requirement-set-detail",
                        pathParameters(parameterWithName("id").description("세트 ID")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.id").description("세트 ID"),
                                fieldWithPath("result.departmentId").description("학과 ID"),
                                fieldWithPath("result.departmentName")
                                        .optional()
                                        .description("학과명 (없으면 null)"),
                                fieldWithPath("result.yearStart").description("적용 시작년도"),
                                fieldWithPath("result.yearEnd").description("적용 종료년도"),
                                fieldWithPath("result.version").description("버전"),
                                fieldWithPath("result.description").optional().description("설명 (없으면 null)"),
                                fieldWithPath("result.sheetImageUrl").optional().description("시트 이미지 URL (없으면 null)"),
                                fieldWithPath("result.active").description("활성 여부"),
                                fieldWithPath("result.graduationRuleIds").description("연결된 졸업 규칙 ID 목록"))));
    }

    @Test
    void 졸업_요건_세트를_생성한다() throws Exception {
        RequirementSetRequest request = new RequirementSetRequest(
                "첨단융합대학", "컴퓨터·AI학부", 2023, 2025, "23~25학번 졸업 요건", "https://img/sheet.png", true, List.of(1L, 2L));
        given(requirementSetCommandService.create(any(RequirementSetCommand.class)))
                .willReturn(100L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/requirement-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(100))
                .andDo(document(
                        "admin-requirement-set-create",
                        requestFields(
                                fieldWithPath("collegeName").description("단과대명(있으면 재사용, 없으면 학과 신규 등록)"),
                                fieldWithPath("departmentName").description("학과명(있으면 재사용, 없으면 학과 신규 등록)"),
                                fieldWithPath("yearStart").description("적용 시작년도"),
                                fieldWithPath("yearEnd").description("적용 종료년도"),
                                fieldWithPath("description").optional().description("설명 (선택)"),
                                fieldWithPath("sheetImageUrl").optional().description("시트 이미지 URL (선택)"),
                                fieldWithPath("active").optional().description("활성 여부 (미지정 시 true)"),
                                fieldWithPath("graduationRuleIds").optional().description("연결할 졸업 규칙 ID 목록")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("생성된 세트 ID"))));
    }

    @Test
    void 졸업_요건_세트를_수정한다() throws Exception {
        RequirementSetUpdateRequest request =
                new RequirementSetUpdateRequest(2023, 2026, "수정된 설명", null, false, List.of(1L, 3L));

        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/admin/requirement-sets/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty())
                .andDo(document(
                        "admin-requirement-set-update",
                        pathParameters(parameterWithName("id").description("세트 ID")),
                        requestFields(
                                fieldWithPath("yearStart").description("적용 시작년도"),
                                fieldWithPath("yearEnd").description("적용 종료년도"),
                                fieldWithPath("description").optional().description("설명 (선택)"),
                                fieldWithPath("sheetImageUrl").optional().description("시트 이미지 URL (선택)"),
                                fieldWithPath("active").optional().description("활성 여부 (미지정 시 true)"),
                                fieldWithPath("graduationRuleIds").optional().description("연결할 졸업 규칙 ID 목록")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("없음"))));
    }
}
