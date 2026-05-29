package com.donggree.graduation.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.graduation.internal.application.GraduationReportService;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.AreaOverview;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.Summary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.payload.JsonFieldType;

class GraduationReportControllerTest extends RestDocsSupport {

    private final GraduationReportService graduationReportService = Mockito.mock(GraduationReportService.class);

    @Override
    protected Object initController() {
        return new GraduationReportController(graduationReportService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 학업_리포트를_조회한다() throws Exception {
        Summary summary =
                new Summary(72, 98, 130, 32, new BigDecimal("3.50"), false, List.of("총 평점평균이 2.0 이상이어야 합니다."));

        List<AreaOverview> areaOverviews = List.of(
                new AreaOverview("COMMON_GENERAL", "공통교양", 100, 0, true),
                new AreaOverview("ACADEMIC_FOUNDATION", "학문기초", 90, 3, false),
                new AreaOverview("FIRST_MAJOR", "제1전공", 80, 12, false));

        given(graduationReportService.getReport(1L)).willReturn(new GraduationReportResponse(summary, areaOverviews));

        mockMvc.perform(get("/api/reports/{reportId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andDo(document(
                        "graduation-get-report",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.summary.achievementRate").description("전체 졸업 달성률 (0~100)"),
                                fieldWithPath("result.summary.earnedCredits").description("현재 이수 학점"),
                                fieldWithPath("result.summary.targetCredits").description("목표 이수 학점"),
                                fieldWithPath("result.summary.remainingCredits").description("잔여 학점"),
                                fieldWithPath("result.summary.gpa")
                                        .type(JsonFieldType.NUMBER)
                                        .description("총 평점 평균"),
                                fieldWithPath("result.summary.graduated").description("졸업 판정 여부"),
                                fieldWithPath("result.summary.unsatisfiedReasons")
                                        .description("졸업요건 부문 미충족 사유 목록"),
                                fieldWithPath("result.areaOverviews[].courseType")
                                        .description("이수 구분 코드 (COMMON_GENERAL, ACADEMIC_FOUNDATION 등)"),
                                fieldWithPath("result.areaOverviews[].courseTypeName")
                                        .description("이수 구분 한국어 명칭"),
                                fieldWithPath("result.areaOverviews[].achievementRate")
                                        .description("해당 영역의 학점 달성률 (0~100)"),
                                fieldWithPath("result.areaOverviews[].remainingCredits")
                                        .description("해당 영역의 잔여 학점"),
                                fieldWithPath("result.areaOverviews[].satisfied")
                                        .description("해당 영역의 요건 충족 여부"))));
    }

    @Test
    void 존재하지_않는_리포트_조회_시_404를_반환한다() throws Exception {
        given(graduationReportService.getReport(999L))
                .willThrow(new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/reports/{reportId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("GRADUATION404_1"));
    }
}
