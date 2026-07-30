package com.donggree.curriculum.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.internal.application.DepartmentQueryService;
import com.donggree.curriculum.internal.application.projection.CollegeProjection;
import com.donggree.curriculum.internal.application.projection.DepartmentProjection;
import com.donggree.global.support.RestDocsSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminDepartmentControllerTest extends RestDocsSupport {

    private final DepartmentQueryService service = Mockito.mock(DepartmentQueryService.class);

    @Override
    protected Object initController() {
        return new AdminDepartmentController(service);
    }

    @Test
    void 단과대_목록을_조회한다() throws Exception {
        given(service.getColleges())
                .willReturn(List.of(new CollegeProjection(1L, "공과대학"), new CollegeProjection(2L, "첨단융합대학")));

        mockMvc.perform(get("/api/admin/colleges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[1].id").value(2))
                .andExpect(jsonPath("$.result[1].collegeName").value("첨단융합대학"))
                .andDo(document(
                        "admin-college-list",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("단과대 ID(요청/응답에 사용)"),
                                fieldWithPath("result[].collegeName").description("단과대명"))));
    }

    @Test
    void 학과_목록을_단과대ID로_조회한다() throws Exception {
        given(service.getDepartments(2L)).willReturn(List.of(new DepartmentProjection(10L, 2L, "첨단융합대학", "컴퓨터·AI학부")));

        mockMvc.perform(get("/api/admin/departments").param("collegeId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(10))
                .andExpect(jsonPath("$.result[0].departmentName").value("컴퓨터·AI학부"))
                .andDo(document(
                        "admin-department-list",
                        queryParameters(
                                parameterWithName("collegeId").optional().description("단과대 ID 필터(미지정=전체)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("학과 ID(요청/응답에 사용)"),
                                fieldWithPath("result[].collegeId").description("소속 단과대 ID"),
                                fieldWithPath("result[].collegeName").description("소속 단과대명"),
                                fieldWithPath("result[].departmentName").description("학과명"))));
    }
}
