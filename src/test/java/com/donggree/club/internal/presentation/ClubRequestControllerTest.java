package com.donggree.club.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.club.internal.application.ClubCommandService;
import com.donggree.club.internal.application.ClubRequestQueryService;
import com.donggree.club.internal.application.command.ClubSaveResult;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.MyClubRequestDetailReadModel;
import com.donggree.club.internal.application.readmodel.MyClubRequestReadModel;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ClubRequestControllerTest extends RestDocsSupport {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final MyClubRequestReadModel REJECTED = new MyClubRequestReadModel(
            31L,
            ClubRequestType.REGISTER,
            null,
            "별자리 탐험대",
            ClubRequestStatus.REJECTED,
            AT.minusDays(2),
            AT,
            "활동 사진을 올려주세요.");

    private final ClubRequestQueryService queryService = Mockito.mock(ClubRequestQueryService.class);
    private final ClubCommandService commandService = Mockito.mock(ClubCommandService.class);

    @Override
    protected Object initController() {
        return new ClubRequestController(queryService, commandService);
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
    void 내_요청_내역을_최신순으로_조회한다() throws Exception {
        MyClubRequestReadModel pending = new MyClubRequestReadModel(
                32L, ClubRequestType.EDIT, 3L, "코드나무", ClubRequestStatus.PENDING, AT, null, null);
        given(queryService.getMyRequests(MEMBER_ID)).willReturn(List.of(pending, REJECTED));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/me/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[1].rejectReason").value("활동 사진을 올려주세요."))
                .andDo(document(
                        "club-my-requests",
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result[]").description("내 요청(최신순). 바로 반영된 변경은 없음"))
                                .andWithPrefix("result[].", myRequestFields())));
    }

    @Test
    void 내_요청_상세는_보냈던_값과_연락처_관계를_준다() throws Exception {
        given(queryService.getMyRequest(MEMBER_ID, 31L))
                .willReturn(new MyClubRequestDetailReadModel(
                        REJECTED,
                        ClubRequestPayload.of(ClubFixtures.profile("별자리 탐험대"), 2),
                        "dongle@dgu.ac.kr",
                        ClubRequesterRole.MEMBER));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/me/requests/{requestId}", 31L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.after.name").value("별자리 탐험대"))
                .andExpect(jsonPath("$.result.after.requesterRole").value("MEMBER"))
                .andDo(document(
                        "club-my-request-detail",
                        pathParameters(parameterWithName("requestId").description("요청 ID")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.after")
                                                .description("보냈던 값. 신규 등록·정보 수정은 동아리 정보, 모집 등록은 모집 정보와 같은 모양"))
                                .andWithPrefix("result.", myRequestFields())
                                .andWithPrefix("result.after.", ClubControllerTest.clubRequestFields())));
    }

    @Test
    void 다른_회원의_요청은_404다() throws Exception {
        given(queryService.getMyRequest(MEMBER_ID, 99L))
                .willThrow(new GeneralException(ClubErrorCode.REQUEST_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/me/requests/{requestId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLUB404_2"));
    }

    @Test
    void 정보_요청을_고쳐_다시_보낸다() throws Exception {
        given(commandService.resubmitInfo(eq(MEMBER_ID), eq(31L), any())).willReturn(ClubSaveResult.pending(null));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/clubs/me/requests/{requestId}/resubmit", 31L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ClubControllerTest.CLUB_BODY))
                .andExpect(status().isOk())
                .andDo(document(
                        "club-request-resubmit",
                        pathParameters(parameterWithName("requestId").description("검토 중·반려된 신규 등록·정보 수정 요청 ID")),
                        requestFields(ClubControllerTest.clubRequestFields()),
                        responseFields(ClubControllerTest.saveResultFields())));
    }

    @Test
    void 모집_요청을_고쳐_다시_보낸다() throws Exception {
        given(commandService.resubmitRecruitment(eq(MEMBER_ID), eq(33L), any())).willReturn(ClubSaveResult.pending(3L));

        mockMvc.perform(RestDocumentationRequestBuilders.post(
                                "/api/clubs/me/requests/{requestId}/resubmit-recruitment", 33L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ClubControllerTest.RECRUITMENT_BODY))
                .andExpect(status().isOk())
                .andDo(document(
                        "club-request-resubmit-recruitment",
                        pathParameters(parameterWithName("requestId").description("검토 중·반려된 모집 요청 ID")),
                        requestFields(ClubControllerTest.recruitmentRequestFields()),
                        responseFields(ClubControllerTest.saveResultFields())));
    }

    @Test
    void 처리된_요청은_다시_보낼_수_없다() throws Exception {
        given(commandService.resubmitInfo(eq(MEMBER_ID), eq(30L), any()))
                .willThrow(new GeneralException(ClubErrorCode.REQUEST_CLOSED));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/clubs/me/requests/{requestId}/resubmit", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ClubControllerTest.CLUB_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB409_2"));
    }

    private static List<FieldDescriptor> myRequestFields() {
        return List.of(
                fieldWithPath("id").description("요청 ID"),
                fieldWithPath("type").description("REGISTER(신규 등록)·EDIT(정보 수정)·RECRUITMENT(모집 등록)"),
                fieldWithPath("clubId").optional().description("대상 동아리 ID. 승인 전 신규 등록은 null"),
                fieldWithPath("clubName").description("요청 당시 동아리 이름"),
                fieldWithPath("status").description("PENDING(검토 중)·APPROVED·REJECTED·CANCELED(고쳐 다시 요청해 취소)"),
                fieldWithPath("requestedAt").description("요청 시각"),
                fieldWithPath("processedAt").optional().description("승인·반려·취소 시각"),
                fieldWithPath("rejectReason").optional().description("반려 사유"));
    }
}
