package com.donggree.user.internal.application;

import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 인증 성공 후 JWT를 발급하고 프론트엔드로 리다이렉트하는 핸들러.
 * 액세스 토큰은 리다이렉트 URL 쿼리 파라미터로, 리프레시 토큰은 HttpOnly 쿠키로 전달한다.
 * 리프레시 토큰은 DB에도 저장하여 서버 측 세션 관리를 가능하게 한다.
 */
@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Value("${app.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuthMember oAuthMember = (OAuthMember) authentication.getPrincipal();
        Member member = oAuthMember.getMember();
        Long memberId = member.getId();

        String accessToken =
                jwtTokenProvider.generateAccessToken(memberId, member.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        // 리프레시 토큰을 DB에 저장 (서버 측 세션 관리)
        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);

        // 리프레시 토큰을 HttpOnly 쿠키로 설정 (HTTPS 환경에서 Secure 플래그 자동 적용)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(jwtTokenProvider.getRefreshExpirationSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 액세스 토큰을 쿼리 파라미터로 프론트엔드에 전달
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
