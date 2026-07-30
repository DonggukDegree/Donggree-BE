package com.donggree.curriculum.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.GraduationRuleCommandService;
import com.donggree.curriculum.internal.application.GraduationRuleQueryService;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import com.donggree.curriculum.internal.application.projection.RuleTypeProjection;
import com.donggree.curriculum.internal.presentation.dto.GraduationRuleBatchRequest;
import com.donggree.global.support.RestDocsSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

class AdminGraduationRuleControllerTest extends RestDocsSupport {

    private final GraduationRuleQueryService graduationRuleQueryService =
            Mockito.mock(GraduationRuleQueryService.class);
    private final GraduationRuleCommandService graduationRuleCommandService =
            Mockito.mock(GraduationRuleCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new AdminGraduationRuleController(graduationRuleQueryService, graduationRuleCommandService);
    }

    @Test
    void 규칙_종류를_조회한다() throws Exception {
        given(graduationRuleQueryService.getRuleTypes())
                .willReturn(List.of(
                        new RuleTypeProjection(1L, "TOTAL_CREDITS", null, "총학점 요건"),
                        new RuleTypeProjection(2L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR, "전공 영역 최소학점")));

        mockMvc.perform(get("/api/admin/rule-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].typeName").value("TOTAL_CREDITS"))
                .andDo(document(
                        "admin-rule-type-list",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("규칙 종류 ID"),
                                fieldWithPath("result[].typeName").description("규칙 종류 식별자(evaluator 분기)"),
                                fieldWithPath("result[].courseType").optional().description("이수구분(null이면 졸업요건 규칙)"),
                                fieldWithPath("result[].description").optional().description("설명 (없으면 null)"))));
    }

    @Test
    void 졸업_규칙을_다중_필터로_조회한다() throws Exception {
        given(graduationRuleQueryService.search(List.of(10L), List.of(CourseType.FIRST_MAJOR)))
                .willReturn(List.of(new GraduationRuleProjection(
                        1L, 10L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR, "전공 영역 최소학점", "{\"min\":30}", "전공 요건")));

        mockMvc.perform(get("/api/admin/graduation-rules")
                        .param("ruleTypeIds", "10")
                        .param("courseTypes", "FIRST_MAJOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].typeName").value("MIN_AREA_CREDITS"))
                .andExpect(jsonPath("$.result[0].ruleConfig.min").value(30))
                .andDo(document(
                        "admin-graduation-rule-search",
                        queryParameters(
                                parameterWithName("ruleTypeIds").optional().description("규칙 종류 ID 필터(다중, 미지정=전체)"),
                                parameterWithName("courseTypes").optional().description("이수구분 필터(다중, 미지정=전체)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("졸업 규칙 ID"),
                                fieldWithPath("result[].ruleTypeId").description("규칙 종류 ID"),
                                fieldWithPath("result[].typeName").description("규칙 종류 식별자"),
                                fieldWithPath("result[].courseType").optional().description("이수구분(null이면 졸업요건 규칙)"),
                                fieldWithPath("result[].ruleName").description("규칙 이름"),
                                subsectionWithPath("result[].ruleConfig").description("규칙 설정 JSON 객체(종류별 스키마)"),
                                fieldWithPath("result[].description").optional().description("설명 (없으면 null)"))));
    }

    @Test
    void 졸업_규칙을_배치_업서트한다() throws Exception {
        GraduationRuleBatchRequest request = new GraduationRuleBatchRequest(List.of(
                new GraduationRuleBatchRequest.Item(
                        1L, 10L, "총 취득학점 130 이상", objectMapper.readTree("{\"min\":130}"), "총학점 요건"),
                new GraduationRuleBatchRequest.Item(
                        null, 20L, "전공 30학점 이상", objectMapper.readTree("{\"min\":30}"), null)));
        given(graduationRuleCommandService.upsert(Mockito.anyList())).willReturn(List.of(1L, 100L));

        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/admin/graduation-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0]").value(1))
                .andExpect(jsonPath("$.result[1]").value(100))
                .andDo(document(
                        "admin-graduation-rule-upsert",
                        requestFields(
                                fieldWithPath("items[].id").optional().description("수정 대상 규칙 ID (null이면 신규 등록)"),
                                fieldWithPath("items[].ruleTypeId").description("규칙 종류 ID"),
                                fieldWithPath("items[].ruleName").description("규칙 이름"),
                                subsectionWithPath("items[].ruleConfig").description("규칙 설정 JSON 객체(종류별 스키마)"),
                                fieldWithPath("items[].description").optional().description("설명 (선택)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[]").description("각 항목의 결과 ID 목록(입력 순서)"))));
    }
}
