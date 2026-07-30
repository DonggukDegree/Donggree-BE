package com.donggree.curriculum.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.CourseClassificationCommandService;
import com.donggree.curriculum.internal.application.CourseClassificationQueryService;
import com.donggree.curriculum.internal.application.projection.AreaTypeProjection;
import com.donggree.curriculum.internal.application.projection.CourseClassificationProjection;
import com.donggree.curriculum.internal.presentation.dto.CourseClassificationBatchRequest;
import com.donggree.global.support.RestDocsSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

class AdminCurriculumControllerTest extends RestDocsSupport {

    private final CourseClassificationQueryService courseClassificationQueryService =
            Mockito.mock(CourseClassificationQueryService.class);
    private final CourseClassificationCommandService courseClassificationCommandService =
            Mockito.mock(CourseClassificationCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new AdminCurriculumController(courseClassificationQueryService, courseClassificationCommandService);
    }

    @Test
    void 과목_분류를_다중_필터로_조회한다() throws Exception {
        given(courseClassificationQueryService.search(
                        List.of(10L, 20L), List.of(CourseType.FIRST_MAJOR), List.of(2024)))
                .willReturn(List.of(new CourseClassificationProjection(
                        1L, "CSE2001", "자료구조", 2023, 2025, CourseType.FIRST_MAJOR, 10L, "전공기초", "개론", "물리")));

        mockMvc.perform(get("/api/admin/course-classifications")
                        .param("areaTypeIds", "10", "20")
                        .param("courseTypes", "FIRST_MAJOR")
                        .param("years", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].courseCode").value("CSE2001"))
                .andExpect(jsonPath("$.result[0].tag").value("자료구조"))
                .andExpect(jsonPath("$.result[0].areaName").value("전공기초"))
                .andDo(document(
                        "admin-course-classification-search",
                        queryParameters(
                                parameterWithName("areaTypeIds").optional().description("이수 영역 ID 필터(다중, 미지정=전체)"),
                                parameterWithName("courseTypes").optional().description("이수구분 필터(다중, 미지정=전체)"),
                                parameterWithName("years")
                                        .optional()
                                        .description("적용 입학년도 필터(다중, 하나라도 start≤year≤end)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("과목 분류 ID"),
                                fieldWithPath("result[].courseCode").description("과목코드"),
                                fieldWithPath("result[].tag").description("표시용 자유 텍스트(과목명 등)"),
                                fieldWithPath("result[].studentYearStart").description("적용 시작 입학년도"),
                                fieldWithPath("result[].studentYearEnd").description("적용 종료 입학년도"),
                                fieldWithPath("result[].courseType").description("이수구분"),
                                fieldWithPath("result[].areaTypeId").description("이수 영역 ID (편집용, 없으면 null)"),
                                fieldWithPath("result[].areaName").description("이수 영역 이름 (없으면 null)"),
                                fieldWithPath("result[].subCategory").description("소분류 (없으면 null)"),
                                fieldWithPath("result[].subjectDomain").description("세부도메인 (없으면 null)"))));
    }

    @Test
    void 과목_분류를_배치_업서트한다() throws Exception {
        CourseClassificationBatchRequest request = new CourseClassificationBatchRequest(List.of(
                new CourseClassificationBatchRequest.Item(
                        1L, "CSE2001", "자료구조", 2023, 2026, CourseType.FIRST_MAJOR, 10L, "개론", "물리"),
                new CourseClassificationBatchRequest.Item(
                        null, "CSE2002", "알고리즘", 2023, 2025, CourseType.FIRST_MAJOR, null, null, null)));
        given(courseClassificationCommandService.upsert(Mockito.anyList())).willReturn(List.of(1L, 100L));

        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/admin/course-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0]").value(1))
                .andExpect(jsonPath("$.result[1]").value(100))
                .andDo(document(
                        "admin-course-classification-upsert",
                        requestFields(
                                fieldWithPath("items[].id").optional().description("수정 대상 분류 ID (null이면 신규 등록)"),
                                fieldWithPath("items[].courseCode").description("과목코드"),
                                fieldWithPath("items[].tag").optional().description("표시용 자유 텍스트(과목명 등, 선택)"),
                                fieldWithPath("items[].studentYearStart").description("적용 시작 입학년도"),
                                fieldWithPath("items[].studentYearEnd").description("적용 종료 입학년도"),
                                fieldWithPath("items[].courseType").description("이수구분"),
                                fieldWithPath("items[].areaTypeId").optional().description("이수 영역 ID (선택)"),
                                fieldWithPath("items[].subCategory").optional().description("소분류 (선택)"),
                                fieldWithPath("items[].subjectDomain")
                                        .optional()
                                        .description("세부도메인 (선택)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[]").description("각 항목의 결과 ID 목록(입력 순서)"))));
    }

    @Test
    void 이수_영역을_조회한다() throws Exception {
        given(courseClassificationQueryService.getAreaTypes())
                .willReturn(List.of(new AreaTypeProjection(10L, "전공기초"), new AreaTypeProjection(20L, "기본소양")));

        mockMvc.perform(get("/api/admin/area-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].areaName").value("전공기초"))
                .andDo(document(
                        "admin-area-type-list",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("이수 영역 ID"),
                                fieldWithPath("result[].areaName").description("이수 영역 이름"))));
    }
}
