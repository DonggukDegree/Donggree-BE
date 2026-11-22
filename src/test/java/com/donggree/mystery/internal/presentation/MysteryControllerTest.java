package com.donggree.mystery.internal.presentation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.mystery.internal.application.*;
import com.donggree.mystery.internal.application.readmodel.MysteryTodayReadModel;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MysteryControllerTest extends RestDocsSupport {
    final MysteryQueryService queries = Mockito.mock(MysteryQueryService.class);
    final MysteryCommandService commands = Mockito.mock(MysteryCommandService.class);

    @Override
    protected Object initController() {
        return new MysteryController(queries, commands);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private MysteryTodayReadModel view() {
        var options = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> new MysteryTodayReadModel.Option("O" + i, "후보" + i))
                .toList();
        return new MysteryTodayReadModel(
                LocalDate.of(2026, 11, 19),
                Instant.parse("2026-11-19T15:00:00Z"),
                "NOT_STARTED",
                options,
                options,
                options,
                List.of(),
                new MysteryTodayReadModel.Statistics(0, 0, 0, null, 0, "BRONZE"),
                null);
    }

    @Test
    void 오늘조회는_정답과_내부후보를_노출하지_않는다() throws Exception {
        given(queries.today(1L)).willReturn(view());
        var result = mockMvc.perform(RestDocumentationRequestBuilders.get("/api/games/mystery/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.answer").doesNotExist())
                .andExpect(jsonPath("$.result.options.departments.length()").value(6))
                .andDo(
                        document(
                                "mystery-today",
                                responseFields(
                                        fieldWithPath("isSuccess").description("성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.gameDate").description("한국 표준시 게임 날짜"),
                                        fieldWithPath("result.nextResetAt").description("다음 자정 UTC 시각"),
                                        fieldWithPath("result.title").description("게임명"),
                                        fieldWithPath("result.subtitle").description("사건명"),
                                        fieldWithPath("result.maxAttempts").description("최대 10회"),
                                        fieldWithPath("result.status")
                                                .description("NOT_STARTED / IN_PROGRESS / WON / LOST"),
                                        subsectionWithPath("result.options")
                                                .description("departments/buildings/methods 각각 고정 순서 6개, id/label"),
                                        subsectionWithPath("result.attempts")
                                                .description(
                                                        "오늘 제출 내역: number, guess(departmentId/buildingId/methodId), correct, clueText, clueCategory. 개별 정오·일치 개수 없음"),
                                        subsectionWithPath("result.statistics")
                                                .description(
                                                        "이번 연속 참여 기간의 completedGames, wonGames, successRate(%), averageAttempts(종료판 전체 평균·없으면 null), currentStreak, tier"))))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("remainingDepartment", "case_data", "requestId");
    }

    @Test
    void 추리를_제출한다() throws Exception {
        given(commands.submit(eq(1L), any())).willReturn(view());
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/games/mystery/today/guesses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"gameDate\":\"2026-11-19\",\"requestId\":\"00000000-0000-0000-0000-000000000001\",\"departmentId\":\"D1\",\"buildingId\":\"B1\",\"methodId\":\"M1\"}"))
                .andExpect(status().isOk())
                .andDo(document(
                        "mystery-guess",
                        requestFields(
                                fieldWithPath("gameDate").description("화면이 조회한 게임 날짜, 자정 이후 이전 날짜 제출은 409"),
                                fieldWithPath("requestId").description("UUID 멱등 요청 번호, 응답 유실 시 같은 본문으로 재사용"),
                                fieldWithPath("departmentId").description("오늘의 학과 ID"),
                                fieldWithPath("buildingId").description("오늘의 장소 ID"),
                                fieldWithPath("methodId").description("오늘의 수단 ID"))));
    }

    @Test
    void 불완전한_추리는_400이다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/games/mystery/today/guesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
