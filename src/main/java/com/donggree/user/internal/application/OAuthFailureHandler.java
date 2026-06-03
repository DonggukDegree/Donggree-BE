package com.donggree.user.internal.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 인증 실패 시 처리 핸들러.
 * Spring 기본 동작(백엔드 {@code /login?error}로 리다이렉트)은 SPA 구조에 맞지 않으므로,
 * 실패 원인을 서버 로그로 남기고 프론트엔드로 {@code ?error} 파라미터를 붙여 리다이렉트한다.
 * 구체적인 예외 메시지는 URL에 노출하지 않고 로그에만 기록한다.
 */
@Slf4j
@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.error("[OAuth2LoginFailure] 카카오 로그인 실패: {}", exception.getMessage(), exception);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("error", "login_failed")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
