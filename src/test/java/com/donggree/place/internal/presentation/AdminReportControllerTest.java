package com.donggree.place.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.admin.AdminMemberSummary;
import com.donggree.place.internal.application.admin.AdminReportCommandService;
import com.donggree.place.internal.application.admin.AdminReportModels.EditHistoryItem;
import com.donggree.place.internal.application.admin.AdminReportModels.InfoSnapshot;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportItem;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceSnapshot;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportItem;
import com.donggree.place.internal.application.admin.AdminReportQueryService;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspensionSource;
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

class AdminReportControllerTest extends RestDocsSupport {

    static final LocalDateTime AT = LocalDateTime.of(2026, 11, 13, 10, 0);
    static final AdminMemberSummary REPORTER =
            new AdminMemberSummary(9L, "신고자", "이신고", "2025000009", "경영대학", "경영학과", null);
    static final AdminMemberSummary AUTHOR =
            new AdminMemberSummary(7L, "김아코", "김아코", "2026000001", "AI융합대학", "컴퓨터공학전공", AT.plusDays(30));

    private final AdminReportQueryService queryService = Mockito.mock(AdminReportQueryService.class);
    private final AdminReportCommandService commandService = Mockito.mock(AdminReportCommandService.class);

    @Override
    protected Object initController() {
        return new AdminReportController(queryService, commandService);
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
    void 장소_신고_목록을_상태별로_조회한다() throws Exception {
        given(queryService.getPlaceReports(PlaceReportStatus.PENDING, 0))
                .willReturn(new Page<>(List.of(placeItem()), 1, 0, 20));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/reports/places")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].reporter.departmentName").value("경영학과"))
                .andDo(document(
                        "admin-report-place-list",
                        queryParameters(
                                parameterWithName("status")
                                        .optional()
                                        .description("PENDING·RESOLVED·REJECTED, 생략 시 전체"),
                                parameterWithName("page").optional().description("0부터 시작하는 페이지(20개씩)")),
                        responseFields(pageFields())
                                .andWithPrefix("result.items[].", placeItemFields())
                                .andWithPrefix("result.items[].reporter.", memberFields())));
    }

    @Test
    void 장소_신고_상세에서_수정_이력과_수정자를_본다() throws Exception {
        InfoSnapshot before = new InfoSnapshot("평일 11:00-21:00", null, List.of(new MenuItem("짜장면", 7000)), List.of());
        InfoSnapshot after = new InfoSnapshot("광고 문구", null, List.of(), List.of(PlaceTag.GENUINE));
        given(queryService.getPlaceReport(1L))
                .willReturn(new PlaceReportDetail(
                        placeItem(),
                        new PlaceSnapshot(
                                10L,
                                "동국반점",
                                PlaceCategory.RESTAURANT,
                                PlaceSubCategory.CHINESE,
                                PlaceStatus.VISIBLE,
                                "광고 문구",
                                null,
                                List.of(),
                                List.of(PlaceTag.GENUINE),
                                AT.minusDays(10),
                                AT.minusDays(1)),
                        REPORTER,
                        List.of(new EditHistoryItem(5L, AUTHOR, AT.minusDays(1), before, after)),
                        List.of(placeItem())));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/reports/places/{reportId}", 1L))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.result.editHistories[0].editor.studentId").value("2026000001"))
                .andDo(document(
                        "admin-report-place-detail",
                        pathParameters(parameterWithName("reportId").description("신고 ID")),
                        relaxedResponseFields(
                                fieldWithPath("result.report").description("이 신고(목록 항목과 같은 모양)"),
                                fieldWithPath("result.place").description("장소 현재 정보(숨김 상태·영업시간·설명·메뉴·태그 포함)"),
                                fieldWithPath("result.registrant").optional().description("최초 등록자 회원 행. 탈퇴 시 null"),
                                fieldWithPath("result.editHistories[].historyId")
                                        .description("수정 이력 ID(되돌리기에 사용)"),
                                fieldWithPath("result.editHistories[].editor")
                                        .optional()
                                        .description("수정자 회원 행(이름·학번·단과대·학과). 탈퇴 시 null"),
                                fieldWithPath("result.editHistories[].editedAt").description("수정 시각"),
                                fieldWithPath("result.editHistories[].before").description("수정 전 영업시간·설명·메뉴·태그"),
                                fieldWithPath("result.editHistories[].after").description("수정 후 값"),
                                fieldWithPath("result.placeReports[]").description("같은 장소의 신고 전체(오래된 순)"))));
    }

    @Test
    void 장소_신고를_처리_완료하고_이미_처리한_신고는_409다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/reports/places/{reportId}/resolve", 1L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-report-place-resolve",
                        pathParameters(parameterWithName("reportId").description("신고 ID")),
                        responseFields(emptyResult())));
        then(commandService).should().resolvePlaceReport(1L);

        willThrow(new GeneralException(PlaceErrorCode.REPORT_ALREADY_PROCESSED))
                .given(commandService)
                .rejectPlaceReport(1L);
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/reports/places/{reportId}/reject", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLACE409_7"));
    }

    @Test
    void 리뷰_신고로_작성자를_정지한다() throws Exception {
        given(commandService.suspendByReviewReport(2L, 30, "욕설", 1L))
                .willReturn(new MemberSuspensionRecord(
                        3L,
                        30,
                        SuspensionSource.REPORT,
                        ReportTargetType.REVIEW,
                        2L,
                        "욕설",
                        AT,
                        AT.plusDays(30),
                        1L,
                        null,
                        null));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/reports/reviews/{reportId}/suspensions", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":30,\"note\":\"욕설\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.days").value(30))
                .andDo(document(
                        "admin-report-review-suspend",
                        pathParameters(parameterWithName("reportId").description("리뷰 신고 ID")),
                        requestFields(
                                fieldWithPath("memberId")
                                        .optional()
                                        .type(org.springframework.restdocs.payload.JsonFieldType.NUMBER)
                                        .description("장소 신고에서만 사용(정지 대상 회원 ID)"),
                                fieldWithPath("days").description("정지 일수(1일 이상, 상한 없음)"),
                                fieldWithPath("note").optional().description("관리자 메모(최대 200자)")),
                        responseFields(suspensionFields())));
    }

    @Test
    void 리뷰_신고_목록에_신고자와_작성자_회원_행을_담는다() throws Exception {
        given(queryService.getReviewReports(null, 0))
                .willReturn(new Page<>(
                        List.of(new ReviewReportItem(
                                2L,
                                ReviewReportReason.ABUSE,
                                "욕설이 있어요",
                                PlaceReportStatus.PENDING,
                                AT,
                                null,
                                REPORTER,
                                30L,
                                1,
                                "별로예요 다시는 안 가요",
                                PlaceReviewStatus.VISIBLE,
                                false,
                                AUTHOR,
                                10L,
                                "동국반점",
                                1)),
                        1,
                        0,
                        20));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/reports/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].author.collegeName").value("AI융합대학"))
                .andDo(document(
                        "admin-report-review-list",
                        relaxedResponseFields(
                                fieldWithPath("result.items[].reportId").description("신고 ID"),
                                fieldWithPath("result.items[].reason").description("사유 코드"),
                                fieldWithPath("result.items[].status").description("PENDING·RESOLVED·REJECTED"),
                                fieldWithPath("result.items[].reviewId").description("리뷰 ID"),
                                fieldWithPath("result.items[].content").description("리뷰 본문"),
                                fieldWithPath("result.items[].reviewStatus").description("VISIBLE·HIDDEN"),
                                fieldWithPath("result.items[].reviewDeleted").description("작성자가 지웠는지"),
                                fieldWithPath("result.items[].author")
                                        .optional()
                                        .description("리뷰 작성자 회원 행. 탈퇴 시 null"),
                                fieldWithPath("result.items[].reporter")
                                        .optional()
                                        .description("신고자 회원 행. 탈퇴 시 null"),
                                fieldWithPath("result.items[].pendingCount").description("이 리뷰의 처리 대기 신고 수"))));
    }

    static PlaceReportItem placeItem() {
        return new PlaceReportItem(
                1L,
                PlaceReportReason.INAPPROPRIATE,
                "광고 문구로 바뀌었어요",
                PlaceReportStatus.PENDING,
                AT,
                null,
                REPORTER,
                10L,
                "동국반점",
                PlaceCategory.RESTAURANT,
                PlaceStatus.VISIBLE,
                1);
    }

    static FieldDescriptor[] memberFields() {
        return new FieldDescriptor[] {
            fieldWithPath("memberId").description("회원 ID"),
            fieldWithPath("nickname").optional().description("닉네임"),
            fieldWithPath("name").optional().description("이름"),
            fieldWithPath("studentId").optional().description("학번"),
            fieldWithPath("collegeName").optional().description("단과대(성적표 주전공 기준, 없으면 null)"),
            fieldWithPath("departmentName").optional().description("학과(성적표 주전공 기준, 없으면 null)"),
            fieldWithPath("suspendedUntil").optional().description("정지 중이면 종료 시각")
        };
    }

    private static FieldDescriptor[] placeItemFields() {
        return new FieldDescriptor[] {
            fieldWithPath("reportId").description("신고 ID"),
            fieldWithPath("reason").description("사유 코드"),
            fieldWithPath("description").optional().description("신고 내용"),
            fieldWithPath("status").description("PENDING·RESOLVED·REJECTED"),
            fieldWithPath("createdAt").description("접수 시각"),
            fieldWithPath("processedAt").optional().description("처리 시각"),
            fieldWithPath("reporter").optional().description("신고자 회원 행. 탈퇴 시 null"),
            fieldWithPath("placeId").description("장소 ID"),
            fieldWithPath("placeName").optional().description("장소 이름"),
            fieldWithPath("category").optional().description("카테고리 코드"),
            fieldWithPath("placeStatus").optional().description("VISIBLE·HIDDEN"),
            fieldWithPath("pendingCount").description("이 장소의 처리 대기 신고 수")
        };
    }

    static FieldDescriptor[] pageFields() {
        return new FieldDescriptor[] {
            fieldWithPath("isSuccess").description("요청 성공 여부"),
            fieldWithPath("code").description("응답 코드"),
            fieldWithPath("message").description("응답 메시지"),
            fieldWithPath("result.items[]").description("목록"),
            fieldWithPath("result.totalCount").description("전체 개수"),
            fieldWithPath("result.page").description("현재 페이지(0부터)"),
            fieldWithPath("result.size").description("페이지 크기")
        };
    }

    static FieldDescriptor[] suspensionFields() {
        return new FieldDescriptor[] {
            fieldWithPath("isSuccess").description("요청 성공 여부"),
            fieldWithPath("code").description("응답 코드"),
            fieldWithPath("message").description("응답 메시지"),
            fieldWithPath("result.id").description("정지 이력 ID"),
            fieldWithPath("result.days").description("정지 일수"),
            fieldWithPath("result.source").description("REPORT(신고 처리)·ADMIN(관리자 판단)"),
            fieldWithPath("result.reportType").optional().description("근거 신고 종류 PLACE·REVIEW"),
            fieldWithPath("result.reportId").optional().description("근거 신고 ID"),
            fieldWithPath("result.note").optional().description("관리자 메모"),
            fieldWithPath("result.startsAt").description("정지 시작 시각"),
            fieldWithPath("result.endsAt").description("정지 종료 시각(이때부터 다시 이용 가능)"),
            fieldWithPath("result.createdBy").description("처리 관리자 회원 ID"),
            fieldWithPath("result.liftedAt").optional().description("해제 시각"),
            fieldWithPath("result.liftedBy").optional().description("해제 관리자 회원 ID")
        };
    }

    private static FieldDescriptor[] emptyResult() {
        return new FieldDescriptor[] {
            fieldWithPath("isSuccess").description("요청 성공 여부"),
            fieldWithPath("code").description("응답 코드"),
            fieldWithPath("message").description("응답 메시지"),
            fieldWithPath("result").optional().description("없음")
        };
    }
}
