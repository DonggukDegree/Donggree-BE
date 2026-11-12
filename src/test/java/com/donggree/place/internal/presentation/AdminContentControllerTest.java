package com.donggree.place.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.application.admin.AdminContentQueryService;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceRow;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewRow;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminContentControllerTest extends RestDocsSupport {

    private final AdminContentQueryService queryService = Mockito.mock(AdminContentQueryService.class);

    @Override
    protected Object initController() {
        return new AdminContentController(queryService);
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
    void 전체_장소_목록을_상태와_이름으로_조회한다() throws Exception {
        given(queryService.getPlaces(PlaceStatus.HIDDEN, "반점", 0))
                .willReturn(new Page<>(
                        List.of(new PlaceRow(
                                10L,
                                "동국반점",
                                PlaceCategory.RESTAURANT,
                                PlaceSubCategory.CHINESE,
                                PlaceStatus.HIDDEN,
                                3,
                                AdminReportControllerTest.AUTHOR,
                                AdminReportControllerTest.AT)),
                        1,
                        0,
                        20));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/places")
                        .param("status", "HIDDEN")
                        .param("keyword", "반점")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].pendingReportCount").value(3))
                .andExpect(jsonPath("$.result.items[0].registrant.studentId").value("2026000001"))
                .andDo(document(
                        "admin-place-list",
                        queryParameters(
                                parameterWithName("status").optional().description("VISIBLE·HIDDEN, 생략 시 전체"),
                                parameterWithName("keyword").optional().description("장소 이름 부분 일치(생략 시 전체)"),
                                parameterWithName("page").optional().description("0부터 시작하는 페이지(20개씩)")),
                        responseFields(AdminReportControllerTest.pageFields())
                                .andWithPrefix(
                                        "result.items[].",
                                        fieldWithPath("placeId").description("장소 ID"),
                                        fieldWithPath("name").description("장소 이름"),
                                        fieldWithPath("category").description("카테고리 코드"),
                                        fieldWithPath("subCategory").description("상세카테고리 코드"),
                                        fieldWithPath("status").description("VISIBLE·HIDDEN"),
                                        fieldWithPath("pendingReportCount").description("처리 대기 신고 수"),
                                        fieldWithPath("registrant").optional().description("등록자 회원 행. 탈퇴 시 null"),
                                        fieldWithPath("createdAt").description("등록 시각"))
                                .andWithPrefix(
                                        "result.items[].registrant.", AdminReportControllerTest.memberFields())));
    }

    @Test
    void 전체_리뷰_목록은_숨긴_리뷰도_본문까지_보여준다() throws Exception {
        given(queryService.getReviews(AdminReviewState.HIDDEN, null, 0))
                .willReturn(new Page<>(
                        List.of(new ReviewRow(
                                30L,
                                10L,
                                "동국반점",
                                1,
                                "광고 문구입니다",
                                PlaceReviewStatus.HIDDEN,
                                false,
                                2,
                                AdminReportControllerTest.AUTHOR,
                                AdminReportControllerTest.AT)),
                        1,
                        0,
                        20));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/reviews")
                        .param("state", "HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].content").value("광고 문구입니다"))
                .andDo(document(
                        "admin-review-list",
                        queryParameters(
                                parameterWithName("state")
                                        .optional()
                                        .description("VISIBLE(보임)·HIDDEN(숨김)·DELETED(작성자 삭제), 생략 시 전체"),
                                parameterWithName("keyword").optional().description("리뷰 본문 또는 장소 이름 부분 일치(생략 시 전체)"),
                                parameterWithName("page").optional().description("0부터 시작하는 페이지(20개씩)")),
                        responseFields(AdminReportControllerTest.pageFields())
                                .andWithPrefix(
                                        "result.items[].",
                                        fieldWithPath("reviewId").description("리뷰 ID"),
                                        fieldWithPath("placeId").description("장소 ID"),
                                        fieldWithPath("placeName").optional().description("장소 이름"),
                                        fieldWithPath("rating").description("별점 1~5"),
                                        fieldWithPath("content").description("본문(숨김·삭제여도 관리자는 본다)"),
                                        fieldWithPath("status").description("VISIBLE·HIDDEN"),
                                        fieldWithPath("deleted").description("작성자가 지웠는지"),
                                        fieldWithPath("pendingReportCount").description("처리 대기 신고 수"),
                                        fieldWithPath("author").optional().description("작성자 회원 행. 탈퇴 시 null"),
                                        fieldWithPath("createdAt").description("작성 시각"))
                                .andWithPrefix("result.items[].author.", AdminReportControllerTest.memberFields())));
    }
}
