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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.PlaceReviewCommandService;
import com.donggree.place.internal.application.PlaceReviewQueryService;
import com.donggree.place.internal.application.ReviewReportCommandService;
import com.donggree.place.internal.application.command.ReportReviewCommand;
import com.donggree.place.internal.application.command.WriteReviewCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceReviewPageReadModel;
import com.donggree.place.internal.application.readmodel.PlaceReviewReadModel;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
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

class PlaceReviewControllerTest extends RestDocsSupport {

    private static final Long MEMBER_ID = 1L;
    private static final String REVIEW_BODY = "{\"rating\":4,\"content\":\"짜장면이 맛있고 양이 많아요.\"}";

    private final PlaceReviewQueryService queryService = Mockito.mock(PlaceReviewQueryService.class);
    private final PlaceReviewCommandService commandService = Mockito.mock(PlaceReviewCommandService.class);
    private final ReviewReportCommandService reportService = Mockito.mock(ReviewReportCommandService.class);

    @Override
    protected Object initController() {
        return new PlaceReviewController(queryService, commandService, reportService);
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
    void 리뷰_목록을_커서로_조회한다() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 11, 12, 18, 0);
        PlaceReviewReadModel other =
                new PlaceReviewReadModel(31L, 5, "짜장면이 맛있고 양이 많아요.", at, at, "김아코", null, false, false);
        PlaceReviewReadModel withdrawn =
                new PlaceReviewReadModel(30L, 3, "점심시간엔 조금 기다려야 해요.", at, at, null, null, false, false);
        PlaceReviewReadModel mine =
                new PlaceReviewReadModel(20L, 4, "탕수육이 바삭해요. 또 올게요.", at, at, "동그리", null, true, false);
        given(queryService.getReviews(MEMBER_ID, 1L, 40L))
                .willReturn(new PlaceReviewPageReadModel(List.of(other, withdrawn), 30L, mine));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/places/{placeId}/reviews", 1L)
                        .param("cursor", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nextCursor").value(30))
                .andExpect(jsonPath("$.result.reviews[1].author.nickname").isEmpty())
                .andExpect(jsonPath("$.result.myReview.mine").value(true))
                .andDo(document(
                        "place-review-list",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        queryParameters(
                                parameterWithName("cursor").optional().description("이전 페이지의 nextCursor. 첫 페이지는 생략")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.reviews[]").description("보이는 리뷰 최신순 최대 10개"),
                                        fieldWithPath("result.nextCursor")
                                                .optional()
                                                .description("다음 페이지 커서. 마지막 페이지면 null"),
                                        fieldWithPath("result.myReview")
                                                .optional()
                                                .description("요청 회원의 리뷰(숨겨졌어도 포함). 없으면 null"))
                                .andWithPrefix("result.reviews[].", reviewFields())
                                .andWithPrefix("result.myReview.", reviewFields())));
    }

    @Test
    void 리뷰를_작성한다() throws Exception {
        given(commandService.write(MEMBER_ID, 1L, new WriteReviewCommand(4, "짜장면이 맛있고 양이 많아요.")))
                .willReturn(30L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reviews", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(30))
                .andDo(document(
                        "place-review-write",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        requestFields(reviewRequestFields()),
                        responseFields(envelope("생성된 리뷰 ID"))));
    }

    @Test
    void 리뷰를_수정한다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/places/{placeId}/reviews/{reviewId}", 1L, 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_BODY))
                .andExpect(status().isOk())
                .andDo(document(
                        "place-review-edit",
                        pathParameters(
                                parameterWithName("placeId").description("장소 ID"),
                                parameterWithName("reviewId").description("리뷰 ID")),
                        requestFields(reviewRequestFields()),
                        responseFields(envelope("없음"))));

        then(commandService).should().edit(MEMBER_ID, 1L, 30L, new WriteReviewCommand(4, "짜장면이 맛있고 양이 많아요."));
    }

    @Test
    void 리뷰를_삭제한다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/places/{placeId}/reviews/{reviewId}", 1L, 30L))
                .andExpect(status().isOk())
                .andDo(document(
                        "place-review-delete",
                        pathParameters(
                                parameterWithName("placeId").description("장소 ID"),
                                parameterWithName("reviewId").description("리뷰 ID")),
                        responseFields(envelope("없음"))));

        then(commandService).should().delete(MEMBER_ID, 1L, 30L);
    }

    @Test
    void 리뷰를_신고한다() throws Exception {
        given(reportService.report(MEMBER_ID, 1L, 30L, new ReportReviewCommand(ReviewReportReason.ABUSE, "욕설이 있어요")))
                .willReturn(60L);

        mockMvc.perform(RestDocumentationRequestBuilders.post(
                                "/api/places/{placeId}/reviews/{reviewId}/reports", 1L, 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ABUSE\",\"description\":\"욕설이 있어요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(60))
                .andDo(document(
                        "place-review-report",
                        pathParameters(
                                parameterWithName("placeId").description("장소 ID"),
                                parameterWithName("reviewId").description("리뷰 ID")),
                        requestFields(
                                fieldWithPath("reason")
                                        .description("신고 사유: ABUSE(욕설·비방), SPAM(광고·도배), FALSE_INFO(허위 내용), "
                                                + "PRIVACY(개인정보 노출), OTHER(기타)"),
                                fieldWithPath("description").optional().description("설명(최대 200자). 기타 사유는 필수")),
                        responseFields(envelope("접수된 신고 ID"))));
    }

    @Test
    void 이미_리뷰를_썼으면_409_4이고_별점이_범위를_벗어나면_400이다() throws Exception {
        given(commandService.write(eq(MEMBER_ID), eq(1L), any(WriteReviewCommand.class)))
                .willThrow(new GeneralException(PlaceErrorCode.DUPLICATE_REVIEW));
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places/{placeId}/reviews", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLACE409_4"));

        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/places/{placeId}/reviews/{reviewId}", 1L, 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"content\":\"짜장면이 맛있고 양이 많아요.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID400_1"));
        then(commandService).should(never()).edit(any(), any(), any(), any());
    }

    private static FieldDescriptor[] reviewFields() {
        return new FieldDescriptor[] {
            fieldWithPath("id").description("리뷰 ID"),
            fieldWithPath("rating").description("별점(1~5)"),
            fieldWithPath("content").description("본문"),
            fieldWithPath("createdAt").description("작성 시각"),
            fieldWithPath("updatedAt").description("마지막 수정 시각"),
            fieldWithPath("author.nickname").optional().description("작성자 닉네임. 탈퇴 등으로 없으면 null(\"탈퇴한 회원\")"),
            fieldWithPath("author.profileUrl").optional().description("작성자 프로필 사진. 없으면 null(기본 아바타)"),
            fieldWithPath("mine").description("요청 회원의 리뷰인지"),
            fieldWithPath("hidden").description("신고·관리자로 숨겨졌는지(본인 리뷰에서만 true 가능)")
        };
    }

    private static FieldDescriptor[] reviewRequestFields() {
        return new FieldDescriptor[] {
            fieldWithPath("rating").description("별점(1~5 정수)"),
            fieldWithPath("content").description("본문(앞뒤 공백 제외 10~500자)")
        };
    }

    private static FieldDescriptor[] envelope(String result) {
        return new FieldDescriptor[] {
            fieldWithPath("isSuccess").description("요청 성공 여부"),
            fieldWithPath("code").description("응답 코드"),
            fieldWithPath("message").description("응답 메시지"),
            fieldWithPath("result").optional().description(result)
        };
    }
}
