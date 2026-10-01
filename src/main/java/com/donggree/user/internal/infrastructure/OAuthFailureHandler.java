package com.donggree.user.internal.infrastructure;

import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.global.metrics.OperationMetricsFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 인증 실패 시 처리 핸들러.
 * Spring 기본 동작(백엔드 {@code /login?error}로 리다이렉트)은 SPA 구조에 맞지 않으므로,
 * 실패 원인을 서버 로그로 남기고 프론트엔드로 {@code ?error} 파라미터를 붙여 리다이렉트한다.
 * 외부 응답 원문을 노출하지 않고 예외 종류와 코드 위치를 기록한다.
 */
@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        RequestDiagnostics.failure(request, "LOGIN_FAILURE", "oauth_callback", "카카오 인증 처리 실패", exception);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("error", "login_failed")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
        request.setAttribute(OperationMetricsFilter.CODE, "LOGIN_FAILURE");
        request.setAttribute(OperationMetricsFilter.OUTCOME, "failure");
    }
}
