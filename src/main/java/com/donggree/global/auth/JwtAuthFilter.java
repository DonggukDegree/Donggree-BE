package com.donggree.global.auth;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.global.metrics.OperationMetricsContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 액세스 토큰을 검증하고 SecurityContext에 인증 객체를 설정하는 필터.
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증한 뒤,
 * 유효하면 memberId를 principal로 하는 Authentication을 생성한다.
 * 인증은 토큰 클레임만으로 하고, 정지 회원 차단을 위해 회원 ID로 이용 제한만 한 번 확인한다.
 * 정지 중이면 요청을 진행하지 않고 403(AUTH403_2)과 정지 종료 시각을 돌려준다(기존 로그인도 즉시 차단).
 *
 * <p>Spring 빈이 아닌 일반 객체로 생성하여 CGLIB 프록시 충돌(GenericFilterBean.init final 메서드)을 방지한다.
 * SecurityConfig에서 직접 인스턴스를 생성하여 SecurityFilterChain에 등록한다.</p>
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberAccessGuard accessGuard; // 없으면(일부 테스트) 정지 확인을 건너뛴다
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this(jwtTokenProvider, null, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰을 한 번만 파싱(서명 검증 포함)하여 memberId와 role을 함께 얻는다.
        var parsed = jwtTokenProvider.parseAccessToken(token);
        if (parsed.isPresent() && accessGuard != null) {
            var restriction = accessGuard.findRestriction(parsed.get().memberId());
            if (restriction.isPresent()) {
                RequestDiagnostics.failure(
                        request, GeneralErrorCode.MEMBER_SUSPENDED.getCode(), "authentication", "정지 회원 요청 차단", null);
                response.setStatus(GeneralErrorCode.MEMBER_SUSPENDED.getStatus().value());
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(
                        response.getWriter(),
                        ApiResponse.onFailure(GeneralErrorCode.MEMBER_SUSPENDED, restriction.get()));
                return;
            }
        }
        parsed.ifPresent(claims -> {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.memberId(), null, toAuthorities(claims.role()));
            // 고유 회원번호 대신 제한된 역할 분류만 운영 지표에 사용한다.
            request.setAttribute(
                    OperationMetricsContext.ACTOR,
                    "ADMIN".equals(claims.role()) || "SUPER_ADMIN".equals(claims.role())
                            ? "admin"
                            : "STUDENT".equals(claims.role()) ? "student" : "unknown");
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        });

        filterChain.doFilter(request, response);
    }

    /**
     * role 클레임을 Spring Security 권한으로 변환한다.
     * {@code hasRole("ADMIN")}이 {@code ROLE_ADMIN} 권한을 요구하므로 ROLE_ 접두사를 붙인다.
     * role이 없는 토큰(구버전 등)이면 권한 없는 빈 목록을 반환한다.
     */
    private List<GrantedAuthority> toAuthorities(String role) {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
