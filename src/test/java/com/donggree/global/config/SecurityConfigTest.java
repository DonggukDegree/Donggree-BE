package com.donggree.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.transcript.internal.application.TranscriptService;
import com.donggree.user.MemberIdentityService;
import com.donggree.user.internal.application.AuthService;
import com.donggree.user.internal.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TranscriptService transcriptService;

    @MockitoBean
    private CurriculumLookupService curriculumLookupService;

    @MockitoBean
    private MemberIdentityService memberIdentityService;

    @MockitoBean
    private com.donggree.graduation.internal.application.GraduationReportService graduationReportService;

    @MockitoBean
    private com.donggree.curriculum.internal.application.CourseClassificationAdminService
            courseClassificationAdminService;

    @TestConfiguration
    static class TestConfig {

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

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(1L, role);
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
