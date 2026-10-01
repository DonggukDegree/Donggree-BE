package com.donggree.global.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.transcript.internal.application.TranscriptCommandService;
import com.donggree.transcript.internal.application.TranscriptQueryService;
import com.donggree.user.MemberIdentityService;
import com.donggree.user.internal.application.AuthService;
import com.donggree.user.internal.application.UserCommandService;
import com.donggree.user.internal.application.UserQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void 요약_API_실제_보안_필터에서도_학생과_관리자_운영_지표를_구분한다() throws Exception {
        meterRegistry.clear();
        for (String role : new String[] {"STUDENT", "ADMIN", "SUPER_ADMIN"}) {
            mockMvc.perform(get("/api/reports/summary")
                            .header("Authorization", "Bearer " + jwtTokenProvider.generateAccessToken(999L, role)))
                    .andExpect(status().isOk());
        }
        org.assertj.core.api.Assertions.assertThat(meterRegistry
                        .get("donggree.operation")
                        .tags("operation", "report_summary", "actor", "student", "code", "COMMON200_1")
                        .timer()
                        .count())
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(meterRegistry
                        .get("donggree.operation")
                        .tags("operation", "report_summary", "actor", "admin")
                        .timer()
                        .count())
                .isEqualTo(2);
        mockMvc.perform(get("/api/reports/summary")).andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(meterRegistry
                        .get("donggree.operation")
                        .tags("http_status", "401", "outcome", "failure", "actor", "unknown")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private UserCommandService userCommandService;

    @MockitoBean
    private TranscriptQueryService transcriptQueryService;

    @MockitoBean
    private TranscriptCommandService transcriptCommandService;

    @MockitoBean
    private CurriculumLookupService curriculumLookupService;

    @MockitoBean
    private MemberIdentityService memberIdentityService;

    @MockitoBean
    private com.donggree.transcript.TranscriptPreviewService transcriptPreviewService;

    @MockitoBean
    private com.donggree.graduation.internal.application.GraduationQueryService graduationQueryService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.CourseClassificationQueryService
            courseClassificationQueryService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.CourseClassificationCommandService
            courseClassificationCommandService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.GraduationRuleQueryService graduationRuleQueryService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.GraduationRuleCommandService graduationRuleCommandService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.RequirementSetQueryService requirementSetQueryService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.RequirementSetCommandService requirementSetCommandService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.DepartmentQueryService departmentQueryService;

    @MockitoBean
    private com.donggree.support.internal.application.FaqQueryService faqQueryService;

    @MockitoBean
    private com.donggree.support.internal.application.FaqCommandService faqCommandService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(new JwtProperties(
                    "test-secret-key-for-unit-testing-only-must-be-at-least-256-bits-long", 1_800_000L, 604_800_000L));
        }

        @Bean
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
            return new DefaultOAuth2UserService();
        }

        @Bean
        AuthenticationSuccessHandler oAuthSuccessHandler() {
            return (request, response, authentication) -> {};
        }

        @Bean
        AuthenticationFailureHandler oAuthFailureHandler() {
            return (request, response, exception) -> {};
        }

        @RestController
        static class TestController {

            @GetMapping("/auth/refresh")
            String authRefresh() {
                return "ok";
            }

            @GetMapping("/auth/logout")
            String authLogout() {
                return "ok";
            }

            @GetMapping("/api/protected")
            String protectedEndpoint() {
                return "protected";
            }

            @GetMapping("/api/admin/ping")
            String adminPing() {
                return "admin";
            }
        }
    }

    @Test
    void 실제_보안실패_로그는_요청ID와_원인코드를_포함한다() throws Exception {
        var logger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(com.donggree.global.logging.RequestDiagnostics.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override
            protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            var result = mockMvc.perform(get("/api/protected").header("Authorization", "Bearer TOKEN_SECRET"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
            org.assertj.core.api.Assertions.assertThat(appender.list).hasSize(1);
            var event = appender.list.getFirst();
            org.assertj.core.api.Assertions.assertThat(event.getMDCPropertyMap())
                    .containsEntry("request_id", result.getResponse().getHeader("X-Request-ID"));
            var fields = event.getKeyValuePairs().stream()
                    .collect(java.util.stream.Collectors.toMap(k -> k.key, k -> k.value));
            org.assertj.core.api.Assertions.assertThat(fields)
                    .containsEntry("code", "AUTH401_1")
                    .containsEntry("auth_reason", "TOKEN_INVALID");
            appender.list.clear();
            mockMvc.perform(get("/api/admin/ping").header("Authorization", bearer("STUDENT")))
                    .andExpect(status().isForbidden());
            org.assertj.core.api.Assertions.assertThat(appender.list).hasSize(1);
            var denied = appender.list.getFirst().getKeyValuePairs().stream()
                    .collect(java.util.stream.Collectors.toMap(k -> k.key, k -> k.value));
            org.assertj.core.api.Assertions.assertThat(denied)
                    .containsEntry("code", "AUTH403_1")
                    .containsEntry("stage", "authorization");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(1L, role);
    }

    @Test
    void PDF_미리보기는_비로그인_접근_차단() throws Exception {
        mockMvc.perform(multipart("/api/admin/reports/preview").file("file", new byte[] {1}))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(transcriptPreviewService, graduationQueryService);
    }

    @Test
    void PDF_미리보기는_학생_접근_차단() throws Exception {
        mockMvc.perform(multipart("/api/admin/reports/preview")
                        .file("file", new byte[] {1})
                        .header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(transcriptPreviewService, graduationQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void PDF_미리보기는_관리자_역할만으로_허용하고_본인확인과_저장_미호출(String role) throws Exception {
        var report = new com.donggree.graduation.internal.application.projection.GraduationReportProjection(
                new com.donggree.graduation.internal.application.projection.GraduationReportProjection.Summary(
                        0, 0, 130, 130, java.math.BigDecimal.ZERO, false, java.util.List.of()),
                java.util.List.of(),
                false,
                null);
        given(graduationQueryService.preview(any()))
                .willReturn(new com.donggree.graduation.internal.application.projection.ReportPreviewProjection(
                        report, java.util.Map.of()));
        mockMvc.perform(multipart("/api/admin/reports/preview")
                        .file("file", new byte[] {1})
                        .header("Authorization", bearer(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.report.summary.targetCredits").value(130));
        verifyNoInteractions(memberIdentityService, transcriptCommandService, userCommandService);
    }

    // role 클레임이 없는 토큰(구버전 액세스 토큰)을 모사한다. 리프레시 토큰은 memberId만 담고 role이 없다.
    private String bearerWithoutRole() {
        return "Bearer " + jwtTokenProvider.generateRefreshToken(1L);
    }

    @Test
    void auth_refresh는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/auth/refresh")).andExpect(status().isOk());
    }

    @Test
    void auth_logout은_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void 인증되지_않은_요청은_401을_ApiResponse_봉투로_반환한다() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void 관리자_경로는_인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
    }

    // FAQ는 비로그인 홈의 푸터에서도 들어올 수 있어야 해서 읽기만 열어 뒀다.
    @Test
    void FAQ_조회는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/faqs")).andExpect(status().isOk());
    }

    // 읽기를 연 것이 쓰기까지 열어버리지 않았는지 확인한다. (쓰기는 /api/admin/faqs)
    @Test
    void FAQ_등록은_인증_없이_접근할_수_없다() throws Exception {
        mockMvc.perform(post("/api/admin/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자_경로는_STUDENT_권한이면_403을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/ping").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자_경로는_ADMIN_권한이면_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/admin/ping").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void 관리자_경로는_SUPER_ADMIN_권한이면_역할_계층에_의해_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/admin/ping").header("Authorization", bearer("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void role이_없는_토큰도_일반_보호_엔드포인트는_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/protected").header("Authorization", bearerWithoutRole()))
                .andExpect(status().isOk());
    }

    @Test
    void role이_없는_토큰은_관리자_경로에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/ping").header("Authorization", bearerWithoutRole()))
                .andExpect(status().isForbidden());
    }
}
