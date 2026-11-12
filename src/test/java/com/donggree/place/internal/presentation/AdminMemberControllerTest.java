package com.donggree.place.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.admin.AdminMemberService;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberRow;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspensionSource;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminMemberControllerTest extends RestDocsSupport {

    private final AdminMemberService memberService = Mockito.mock(AdminMemberService.class);

    @Override
    protected Object initController() {
        return new AdminMemberController(memberService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 회원을_검색해_이름_학번_단과대_학과와_받은_신고_수를_본다() throws Exception {
        given(memberService.searchMembers("김", 0))
                .willReturn(new Page<>(List.of(new MemberRow(AdminReportControllerTest.AUTHOR, 5)), 1, 0, 20));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/members")
                        .param("keyword", "김"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].member.departmentName").value("컴퓨터공학전공"))
                .andDo(document(
                        "admin-member-search",
                        queryParameters(
                                parameterWithName("keyword").optional().description("학번·이름·닉네임 부분 일치(생략 시 전체)"),
                                parameterWithName("page").optional().description("0부터 시작하는 페이지(20개씩)")),
                        responseFields(AdminReportControllerTest.pageFields())
                                .andWithPrefix("result.items[].member.", AdminReportControllerTest.memberFields())
                                .and(
                                        fieldWithPath("result.items[].member").description("회원 행"),
                                        fieldWithPath("result.items[].receivedReportCount")
                                                .description("받은 신고 수(리뷰 신고 + 장소 신고 정지)"))));
    }

    @Test
    void 회원_상세에서_받은_신고와_정지_이력을_본다() throws Exception {
        var at = AdminReportControllerTest.AT;
        given(memberService.getMember(7L))
                .willReturn(new MemberDetail(
                        AdminReportControllerTest.AUTHOR,
                        "STUDENT",
                        at.minusMonths(8),
                        4,
                        1,
                        5,
                        List.of(new MemberSuspensionRecord(
                                3L,
                                30,
                                SuspensionSource.REPORT,
                                ReportTargetType.REVIEW,
                                2L,
                                "욕설",
                                at,
                                at.plusDays(30),
                                1L,
                                null,
                                null))));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/members/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.receivedReportCount").value(5))
                .andDo(document(
                        "admin-member-detail",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.member").description("회원 행"),
                                        fieldWithPath("result.role").description("역할"),
                                        fieldWithPath("result.joinedAt").description("가입 시각"),
                                        fieldWithPath("result.receivedReviewReportCount")
                                                .description("그 회원 리뷰에 들어온 신고 수"),
                                        fieldWithPath("result.placeReportSuspensionCount")
                                                .description("장소 신고 처리로 정지된 횟수"),
                                        fieldWithPath("result.receivedReportCount")
                                                .description("받은 신고 합계"),
                                        fieldWithPath("result.suspensions[]").description("정지 이력(최신순)"),
                                        fieldWithPath("result.suspensions[].id").description("정지 이력 ID"),
                                        fieldWithPath("result.suspensions[].days")
                                                .description("정지 일수"),
                                        fieldWithPath("result.suspensions[].source")
                                                .description("REPORT·ADMIN"),
                                        fieldWithPath("result.suspensions[].reportType")
                                                .optional()
                                                .description("근거 신고 종류"),
                                        fieldWithPath("result.suspensions[].reportId")
                                                .optional()
                                                .description("근거 신고 ID"),
                                        fieldWithPath("result.suspensions[].note")
                                                .optional()
                                                .description("관리자 메모"),
                                        fieldWithPath("result.suspensions[].startsAt")
                                                .description("시작 시각"),
                                        fieldWithPath("result.suspensions[].endsAt")
                                                .description("종료 시각"),
                                        fieldWithPath("result.suspensions[].createdBy")
                                                .description("처리 관리자"),
                                        fieldWithPath("result.suspensions[].liftedAt")
                                                .optional()
                                                .description("해제 시각"),
                                        fieldWithPath("result.suspensions[].liftedBy")
                                                .optional()
                                                .description("해제 관리자"))
                                .andWithPrefix("result.member.", AdminReportControllerTest.memberFields())));
    }

    @Test
    void 관리자가_회원을_정지하고_해제한다() throws Exception {
        var at = AdminReportControllerTest.AT;
        given(memberService.suspend(7L, 7, null, 1L))
                .willReturn(new MemberSuspensionRecord(
                        4L, 7, SuspensionSource.ADMIN, null, null, null, at, at.plusDays(7), 1L, null, null));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/members/{memberId}/suspensions", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":7}"))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-member-suspend",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(AdminReportControllerTest.suspensionFields())));
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/members/{memberId}/suspensions/lift", 7L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-member-lift",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").optional().description("없음"))));
        then(memberService).should().lift(7L, 1L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/members/{memberId}/suspensions", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":0}"))
                .andExpect(status().isBadRequest());
    }
}
