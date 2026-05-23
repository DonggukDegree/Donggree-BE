package com.donggree.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.user.internal.application.AuthService;
import com.donggree.user.internal.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(new JwtProperties(
                    "test-secret-key-for-unit-testing-only-must-be-at-least-256-bits-long",
                    1_800_000L, 604_800_000L
            ));
        }

        @Bean
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
            return new DefaultOAuth2UserService();
        }

        @Bean
        AuthenticationSuccessHandler oAuthSuccessHandler() {
            return (request, response, authentication) -> {
            };
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
        }
    }

    @Test
    void auth_refresh는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/auth/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    void auth_logout은_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증되지_않은_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }
}
