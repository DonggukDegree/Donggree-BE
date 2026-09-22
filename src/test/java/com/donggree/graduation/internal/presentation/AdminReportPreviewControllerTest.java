package com.donggree.graduation.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.graduation.internal.application.GraduationQueryService;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
import com.donggree.graduation.internal.application.projection.ReportPreviewProjection;
import com.donggree.transcript.TranscriptPreviewService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AdminReportPreviewControllerTest extends RestDocsSupport {
    private final TranscriptPreviewService transcriptPreviewService = mock(TranscriptPreviewService.class);
    private final GraduationQueryService graduationQueryService = mock(GraduationQueryService.class);

    @Override
    protected Object initController() {
        return new AdminReportPreviewController(transcriptPreviewService, graduationQueryService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 요약과_전체_영역_상세를_캐시_없이_일괄_반환() throws Exception {
        var report = new GraduationReportProjection(
                new GraduationReportProjection.Summary(0, 3, 130, 127, new BigDecimal("4.0"), false, List.of("총학점 부족")),
                List.of(new GraduationReportProjection.AreaOverview("FIRST_MAJOR", "제1전공", 0, 33, false)),
                false,
                true);
        var detail = new AreaDetailProjection(
                List.of(), List.of("전공 학점 부족"), new AreaDetailProjection.CreditStatus(3, 36, 33));
        given(graduationQueryService.preview(any()))
                .willReturn(new ReportPreviewProjection(report, Map.of(CourseType.FIRST_MAJOR, detail)));
        mockMvc.perform(multipart("/api/admin/reports/preview")
                        .file(new MockMultipartFile("file", "transcript.pdf", "application/pdf", new byte[] {1})))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.result.report.summary.earnedCredits").value(3))
                .andExpect(jsonPath("$.result.details.FIRST_MAJOR.creditStatus.targetCredits")
                        .value(36))
                .andExpect(jsonPath("$.result.memberId").doesNotExist())
                .andExpect(jsonPath("$.result.rawData").doesNotExist())
                .andDo(document(
                        "admin-report-preview",
                        requestParts(partWithName("file").description("미리보기용 취득교과목 영역별 분류표 PDF")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                subsectionWithPath("result.report").description("일반 리포트와 동일한 요약·영역 개요·정확도 경고"),
                                subsectionWithPath("result.details")
                                        .description("개요의 모든 이수구분을 키로 하는 영역 상세. 일반 영역별 리포트와 동일한 구조"))));
    }

    @Test
    void 적용_세트가_없으면_기존_리포트와_같은_오류() throws Exception {
        given(graduationQueryService.preview(any()))
                .willThrow(new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
        mockMvc.perform(multipart("/api/admin/reports/preview").file("file", new byte[] {1}))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRADUATION404_2"));
    }

    @Test
    void 파싱_실패_시_판정_미호출() throws Exception {
        given(transcriptPreviewService.preview(any()))
                .willThrow(new GeneralException(com.donggree.global.apiPayload.code.GeneralErrorCode.BAD_REQUEST));
        mockMvc.perform(multipart("/api/admin/reports/preview").file("file", new byte[] {1}))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(graduationQueryService);
    }
}
