package com.donggree.club.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.club.internal.application.admin.AdminClubCommandService;
import com.donggree.club.internal.application.admin.AdminClubQueryService;
import com.donggree.club.internal.application.admin.AdminClubReadModel;
import com.donggree.club.internal.application.admin.AdminClubRequestReadModel;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubProfile;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;

class AdminClubControllerTest extends RestDocsSupport {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 27, 12, 0);

    private final AdminClubQueryService queryService = Mockito.mock(AdminClubQueryService.class);
    private final AdminClubCommandService commandService = Mockito.mock(AdminClubCommandService.class);

    @Override
    protected Object initController() {
        return new AdminClubController(queryService, commandService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 검토_중_요청을_요청자_정보와_변경_전후_값으로_조회한다() throws Exception {
        ClubProfile before = ClubFixtures.profile("책갈피");
        ClubProfile after = new ClubProfile(
                before.name(),
                before.affiliation(),
                before.fields(),
                before.description(),
                before.generation(),
                before.activityStartDate(),
                before.activityEndDate(),
                null,
                before.generationMemberCount(),
                130,
                before.foundedDate(),
                before.officers(),
                before.links());
        given(queryService.getRequests(ClubRequestStatus.PENDING))
                .willReturn(List.of(new AdminClubRequestReadModel(
                        6L,
                        ClubRequestType.EDIT,
                        4L,
                        "책갈피",
                        ClubRequestStatus.PENDING,
                        AT,
                        null,
                        null,
                        ClubRequestPayload.of(before, 0),
                        ClubRequestPayload.of(after, 1),
                        new AdminClubRequestReadModel.Requester(
                                12L, "박서연", "seoyeon@dgu.ac.kr", ClubRequesterRole.PRESIDENT))));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/clubs/requests")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].requester.name").value("박서연"))
                .andExpect(jsonPath("$.result[0].after.totalMemberCount").value(130))
                .andDo(document(
                        "admin-club-requests",
                        queryParameters(parameterWithName("status")
                                .optional()
                                .description("PENDING(먼저 들어온 순)·APPROVED·REJECTED·CANCELED(최신순). 생략하면 전체(최신순)")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result[].id").description("요청 ID"),
                                        fieldWithPath("result[].type").description("REGISTER·EDIT·RECRUITMENT"),
                                        fieldWithPath("result[].clubId")
                                                .optional()
                                                .description("대상 동아리. 승인 전 신규 등록은 null"),
                                        fieldWithPath("result[].clubName").description("요청 당시 동아리 이름"),
                                        fieldWithPath("result[].status").description("요청 상태"),
                                        fieldWithPath("result[].requestedAt").description("요청 시각"),
                                        fieldWithPath("result[].processedAt")
                                                .optional()
                                                .description("처리 시각"),
                                        fieldWithPath("result[].rejectReason")
                                                .optional()
                                                .description("반려 사유"),
                                        fieldWithPath("result[].before")
                                                .optional()
                                                .description("요청 당시 현재 값(저장 요청 본문과 같은 모양). 신규 등록·새 모집은 null"),
                                        fieldWithPath("result[].after").description("요청 값(저장 요청 본문과 같은 모양)"),
                                        fieldWithPath("result[].requester.memberId")
                                                .optional()
                                                .description("요청 회원 ID. 탈퇴했으면 null"),
                                        fieldWithPath("result[].requester.name")
                                                .optional()
                                                .description("요청자 실명"),
                                        fieldWithPath("result[].requester.contact")
                                                .optional()
                                                .description("확인 연락처. 요청 1년 뒤·탈퇴 시 null"),
                                        fieldWithPath("result[].requester.role")
                                                .optional()
                                                .description("동아리와의 관계. 요청 1년 뒤·탈퇴 시 null"))
                                .andWithPrefix("result[].before.", valueFields())
                                .andWithPrefix("result[].after.", valueFields())));
    }

    @Test
    void 요청을_승인한다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/clubs/requests/{requestId}/approve", 6L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-request-approve",
                        pathParameters(parameterWithName("requestId").description("검토 중 요청 ID")),
                        responseFields(envelope())));
        verify(commandService).approve(6L);
    }

    @Test
    void 사유와_함께_반려하고_처리된_요청은_409다() throws Exception {
        willThrow(new GeneralException(ClubErrorCode.REQUEST_ALREADY_PROCESSED))
                .given(commandService)
                .reject(7L, "다시");

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/clubs/requests/{requestId}/reject", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"활동 사진을 올려주세요.\"}"))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-request-reject",
                        pathParameters(parameterWithName("requestId").description("검토 중 요청 ID")),
                        requestFields(fieldWithPath("reason").description("반려 사유(1~200자). 요청자의 내 요청 내역에 보인다")),
                        responseFields(envelope())));
        verify(commandService).reject(6L, "활동 사진을 올려주세요.");

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/clubs/requests/{requestId}/reject", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"다시\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB409_4"));
    }

    @Test
    void 동아리_목록을_권한_회원과_함께_조회한다() throws Exception {
        ClubSummaryReadModel club = new ClubSummaryReadModel(
                3L,
                "한빛봉사단",
                ClubAffiliation.CENTRAL,
                List.of(ClubField.VOLUNTEER),
                ClubRegistration.OFFICIAL,
                true,
                List.of(),
                80,
                LocalDate.of(2005, 3, 2),
                null);
        given(queryService.getClubs("한빛"))
                .willReturn(List.of(new AdminClubReadModel(
                        club, List.of(new AdminClubReadModel.Editor(8L, "윤다온", AT.minusDays(3))), AT)));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/clubs").param("keyword", "한빛"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].editors[0].name").value("윤다온"))
                .andDo(document(
                        "admin-club-list",
                        queryParameters(parameterWithName("keyword").optional().description("이름 부분 일치. 생략하면 전체")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[]").description("먼저 등록된 순(ID순) 동아리"),
                                fieldWithPath("result[].id").description("동아리 ID"),
                                fieldWithPath("result[].name").description("동아리 이름"),
                                fieldWithPath("result[].affiliation").description("소속 분류"),
                                fieldWithPath("result[].fields[]").description("활동 분야"),
                                fieldWithPath("result[].registration").description("PROVISIONAL·OFFICIAL"),
                                fieldWithPath("result[].activityConfirmed").description("현재 활동 확인"),
                                fieldWithPath("result[].imageUrls[]").description("사진 주소"),
                                fieldWithPath("result[].totalMemberCount")
                                        .optional()
                                        .description("전체 활동 인원"),
                                fieldWithPath("result[].foundedDate").optional().description("창립일"),
                                fieldWithPath("result[].recruitment").optional().description("현재 모집(공개 목록과 같은 모양)"),
                                fieldWithPath("result[].editors[]").description("직접 수정 권한 회원(부여 순)"),
                                fieldWithPath("result[].editors[].memberId").description("회원 ID"),
                                fieldWithPath("result[].editors[].name")
                                        .optional()
                                        .description("실명"),
                                fieldWithPath("result[].editors[].grantedAt").description("권한 부여 시각"),
                                fieldWithPath("result[].updatedAt").description("마지막 정보 등록·수정 시각"))));
    }

    @Test
    void 관리자_임시_등록과_정보_수정과_권한_회수() throws Exception {
        given(commandService.registerProvisional(any())).willReturn(10L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ClubControllerTest.CLUB_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(10))
                .andDo(document(
                        "admin-club-register",
                        requestFields(ClubControllerTest.clubRequestFields()),
                        responseFields(envelope("임시 등록한 동아리 ID"))));
        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/admin/clubs/{clubId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ClubControllerTest.CLUB_BODY))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-edit",
                        pathParameters(parameterWithName("clubId").description("동아리 ID")),
                        requestFields(ClubControllerTest.clubRequestFields()),
                        responseFields(envelope())));
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/admin/clubs/{clubId}/editors/{memberId}", 3L, 8L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-editor-revoke",
                        pathParameters(
                                parameterWithName("clubId").description("동아리 ID"),
                                parameterWithName("memberId").description("권한을 회수할 회원 ID")),
                        responseFields(envelope())));
        verify(commandService).edit(any(), any());
        verify(commandService).revokeEditor(3L, 8L);
    }

    private static List<FieldDescriptor> valueFields() {
        List<FieldDescriptor> fields = new ArrayList<>(ClubControllerTest.clubRequestFields());
        fields.removeIf(field -> field.getPath().equals("officers[].department"));
        fields.add(fieldWithPath("officers[].department").description("학과. 비우면 빈 문자열"));
        return fields.stream()
                .map(field -> field.getPath().startsWith("requester")
                        ? fieldWithPath(field.getPath()).optional().description("관리자 응답에서는 null(requester 참조)")
                        : field)
                .toList();
    }

    private static List<FieldDescriptor> envelope() {
        return envelopeWith(fieldWithPath("result").optional().description("없음(null)"));
    }

    private static List<FieldDescriptor> envelope(String result) {
        return envelopeWith(fieldWithPath("result").description(result));
    }

    private static List<FieldDescriptor> envelopeWith(FieldDescriptor result) {
        return List.of(
                fieldWithPath("isSuccess").description("요청 성공 여부"),
                fieldWithPath("code").description("응답 코드"),
                fieldWithPath("message").description("응답 메시지"),
                result);
    }
}
