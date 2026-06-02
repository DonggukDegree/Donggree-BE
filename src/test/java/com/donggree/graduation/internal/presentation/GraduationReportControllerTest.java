package com.donggree.graduation.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.graduation.internal.application.GraduationReportService;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse.AreaSection;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse.CourseItem;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse.CreditStatus;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.AreaOverview;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.Summary;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long memberId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList()));
    }

    @Test
    void 학업_리포트를_조회한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        Summary summary =
                new Summary(72, 98, 130, 32, new BigDecimal("3.50"), false, List.of("총 평점평균이 2.0 이상이어야 합니다."));

        List<AreaOverview> areaOverviews = List.of(
                new AreaOverview("COMMON_GENERAL", "공통교양", 100, 0, true),
                new AreaOverview("ACADEMIC_FOUNDATION", "학문기초", 90, 3, false),
                new AreaOverview("FIRST_MAJOR", "제1전공", 80, 12, false));

        given(graduationReportService.getReport(memberId))
                .willReturn(new GraduationReportResponse(summary, areaOverviews));

        mockMvc.perform(get("/api/reports/summary"))
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
        Long memberId = 999L;
        authenticate(memberId);

        given(graduationReportService.getReport(memberId))
                .willThrow(new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("GRADUATION404_1"));
    }

    @Test
    void 영역별_이수_현황을_조회한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        List<CourseItem> 동국인성Items =
                List.of(new CourseItem("불교와인간", 2, "SATISFIED", null), new CourseItem("자아와명상1", 2, "OPTIONAL", null));
        List<CourseItem> 자기계발Items = List.of(new CourseItem("진로탐색과비전", 1, "OPTIONAL", null));

        List<AreaSection> areaSections =
                List.of(new AreaSection("동국인성", 4, 0, true, 동국인성Items), new AreaSection("자기계발", 1, 0, true, 자기계발Items));

        AreaDetailResponse response =
                new AreaDetailResponse(areaSections, List.of("EAS2 이전에 EAS1을 선이수해야 합니다."), new CreditStatus(17, 17, 0));

        given(graduationReportService.getAreaDetail(memberId, CourseType.COMMON_GENERAL))
                .willReturn(response);

        mockMvc.perform(get("/api/reports").param("courseType", "COMMON_GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andDo(document(
                        "graduation-get-area-detail",
                        queryParameters(parameterWithName("courseType")
                                .description(
                                        "이수 구분 코드 (COMMON_GENERAL, ACADEMIC_FOUNDATION, FIRST_MAJOR, LIBERAL_ARTS 등)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.areaDetails[].areaName").description("영역(areaType) 이름"),
                                fieldWithPath("result.areaDetails[].earnedCredits")
                                        .description("해당 영역 이수 학점"),
                                fieldWithPath("result.areaDetails[].targetCredits")
                                        .description("해당 영역 최소 목표 학점 (규칙이 없으면 0)"),
                                fieldWithPath("result.areaDetails[].satisfied").description("해당 영역 필수 요건 충족 여부"),
                                fieldWithPath("result.areaDetails[].items[].title")
                                        .description("과목명 또는 소분류 별명"),
                                fieldWithPath("result.areaDetails[].items[].credit")
                                        .description("이수 학점 (미이수 필수과목은 0)"),
                                fieldWithPath("result.areaDetails[].items[].status")
                                        .description("SATISFIED(필수 이수) | UNSATISFIED(필수 미이수) | OPTIONAL(선택 이수)"),
                                fieldWithPath("result.areaDetails[].items[].detail")
                                        .type(JsonFieldType.ARRAY)
                                        .optional()
                                        .description("소분류 별명인 경우 실제 수강 과목명 목록, 단일 과목이면 null"),
                                fieldWithPath("result.unsatisfiedReasons")
                                        .description("해당 courseType에서 미충족된 졸업 규칙 사유 목록"),
                                fieldWithPath("result.creditStatus.earnedCredits")
                                        .description("해당 courseType 총 이수 학점"),
                                fieldWithPath("result.creditStatus.targetCredits")
                                        .description("해당 courseType 최소 목표 학점"),
                                fieldWithPath("result.creditStatus.remainingCredits")
                                        .description("해당 courseType 잔여 학점"))));
    }

    @Test
    void 영역별_이수_현황_조회_시_성적표_없으면_404를_반환한다() throws Exception {
        Long memberId = 999L;
        authenticate(memberId);

        given(graduationReportService.getAreaDetail(memberId, CourseType.COMMON_GENERAL))
                .willThrow(new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/reports").param("courseType", "COMMON_GENERAL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("GRADUATION404_1"));
    }
}
