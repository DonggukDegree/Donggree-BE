package com.donggree.place.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.PlaceReportCommandService;
import com.donggree.place.internal.application.command.ReportPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PlaceReportControllerTest extends RestDocsSupport {

    private static final Long MEMBER_ID = 1L;

    private final PlaceReportCommandService reportCommandService = Mockito.mock(PlaceReportCommandService.class);

    @Override
    protected Object initController() {
        return new PlaceReportController(reportCommandService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(MEMBER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 장소를_신고한다() throws Exception {
        given(reportCommandService.report(
                        eq(MEMBER_ID), eq(1L), eq(new ReportPlaceCommand(PlaceReportReason.CLOSED, "10월부터 영업하지 않아요"))))
                .willReturn(50L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reports", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\",\"description\":\"10월부터 영업하지 않아요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(50))
                .andDo(document(
                        "place-report",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        requestFields(
                                fieldWithPath("reason")
                                        .description("신고 사유: CLOSED(폐업), WRONG_LOCATION(위치 오류), WRONG_NAME(이름 오류), "
                                                + "DUPLICATE(중복 장소), INAPPROPRIATE(부적절한 내용), OTHER(기타)"),
                                fieldWithPath("description").optional().description("설명(최대 200자). 기타 사유는 필수, 그 외 선택")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("접수된 신고 ID"))));
    }

    @Test
    void 처리_대기_신고가_있으면_409_3이다() throws Exception {
        given(reportCommandService.report(eq(MEMBER_ID), eq(1L), any(ReportPlaceCommand.class)))
                .willThrow(new GeneralException(PlaceErrorCode.DUPLICATE_REPORT));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reports", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLACE409_3"));
    }

    @Test
    void 사유가_없거나_설명이_200자를_넘으면_400이다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reports", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"폐업\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID400_1"));
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reports", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\",\"description\":\"" + "가".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID400_1"));

        then(reportCommandService).should(never()).report(any(), any(), any());
    }
}
