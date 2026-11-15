package com.donggree.point.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.point.internal.application.PointQueryService;
import com.donggree.point.internal.application.PointRewardService;
import com.donggree.point.internal.application.readmodel.PointHistoryPageReadModel;
import com.donggree.point.internal.domain.PointReason;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PointControllerTest extends RestDocsSupport {

    private static final Long MEMBER_ID = 1L;

    private final PointQueryService queryService = Mockito.mock(PointQueryService.class);
    private final PointRewardService rewardService = Mockito.mock(PointRewardService.class);

    @Override
    protected Object initController() {
        return new PointController(queryService, rewardService);
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
    void 내_총_포인트를_조회한다() throws Exception {
        given(queryService.getTotalPoints(MEMBER_ID)).willReturn(1100L);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/points/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalPoints").value(1100))
                .andDo(document(
                        "point-summary",
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.totalPoints").description("총 포인트(숨김으로 회수된 적립 제외)"))));
    }

    @Test
    void 방금_한_활동의_적립을_안내했다고_기록한다() throws Exception {
        given(rewardService.acknowledge(MEMBER_ID, PointReason.PLACE_INFO_EDIT, 10L))
                .willReturn(Optional.of(100));

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/points/me/rewards/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"PLACE_INFO_EDIT\",\"targetId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rewarded").value(true))
                .andExpect(jsonPath("$.result.amount").value(100))
                .andDo(document(
                        "point-reward-acknowledge",
                        requestFields(
                                fieldWithPath("reason").description("방금 한 활동의 적립 사유"),
                                fieldWithPath("targetId").optional().description("장소 활동이면 장소 ID, 졸업 리포트 최초 확인이면 생략")),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.rewarded")
                                        .description("이번에 처음 안내하는 적립이면 true. 적립 없음·이미 안내·회수면 false"),
                                fieldWithPath("result.amount").description("적립액. rewarded가 false면 0"))));
    }

    @Test
    void 적립이_없거나_이미_안내했으면_rewarded_false() throws Exception {
        given(rewardService.acknowledge(MEMBER_ID, PointReason.GRADUATION_REPORT_FIRST_VIEW, null))
                .willReturn(Optional.empty());

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/points/me/rewards/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"GRADUATION_REPORT_FIRST_VIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rewarded").value(false))
                .andExpect(jsonPath("$.result.amount").value(0));
    }

    @Test
    void 내_적립_내역을_최신순_커서로_조회한다() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 11, 16, 12, 0);
        given(queryService.getHistories(MEMBER_ID, 40L))
                .willReturn(new PointHistoryPageReadModel(
                        List.of(
                                new PointHistoryPageReadModel.Item(39L, PointReason.PLACE_REGISTER, 500, false, at),
                                new PointHistoryPageReadModel.Item(
                                        35L, PointReason.PLACE_REVIEW, 100, true, at.minusDays(1))),
                        35L));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/points/me/histories")
                        .queryParam("cursor", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.histories[0].reason").value("PLACE_REGISTER"))
                .andExpect(jsonPath("$.result.histories[1].revoked").value(true))
                .andExpect(jsonPath("$.result.nextCursor").value(35))
                .andDo(document(
                        "point-histories",
                        queryParameters(
                                parameterWithName("cursor").optional().description("이전 페이지의 nextCursor. 첫 페이지는 생략")),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.histories[]").description("적립 내역(최신순, 최대 20개)"),
                                fieldWithPath("result.histories[].historyId").description("내역 ID"),
                                fieldWithPath("result.histories[].reason")
                                        .description("적립 사유: GRADUATION_REPORT_FIRST_VIEW(졸업 리포트 첫 확인 500), "
                                                + "PLACE_REGISTER(장소 등록 500), PLACE_FIRST_REVIEW(장소 첫 리뷰 500), "
                                                + "PLACE_REVIEW(리뷰 100), PLACE_INFO_EDIT(장소 정보 수정 100)"),
                                fieldWithPath("result.histories[].amount").description("적립액"),
                                fieldWithPath("result.histories[].revoked")
                                        .description("대상 장소·리뷰가 숨겨져 회수됐는지. true면 총 포인트에서 빠짐"),
                                fieldWithPath("result.histories[].createdAt").description("적립 시각"),
                                fieldWithPath("result.nextCursor")
                                        .type(JsonFieldType.NUMBER)
                                        .optional()
                                        .description("다음 페이지 커서. 마지막 페이지면 null"))));
    }
}
