package com.donggree.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 액세스/리프레시 토큰 생성, 검증, 클레임 추출을 담당하는 컴포넌트.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = properties.accessExpiration();
        this.refreshExpiration = properties.refreshExpiration();
    }

    /**
     * 액세스 토큰 생성. 클레임에 memberId와 권한(role)을 포함한다.
     * role은 인가 판정에 사용되며, {@code "ROLE_" + role} 형태의 권한으로 매핑된다.
     * 모듈 격리를 위해 user 모듈의 Role enum이 아닌 문자열로 전달받는다.
     */
    public String generateAccessToken(Long memberId, String role) {
        return buildToken(accessExpiration, Map.of("memberId", memberId, "role", role));
    }

    /**
     * 리프레시 토큰 생성. 클레임에 memberId를 포함한다. (role 없음 — 갱신 시 DB에서 재조회)
     */
    public String generateRefreshToken(Long memberId) {
        return buildToken(refreshExpiration, Map.of("memberId", memberId));
    }

    /**
     * 리프레시 토큰 만료 시간(초)을 반환한다. 쿠키 maxAge 설정에 사용한다.
     */
    public long getRefreshExpirationSeconds() {
        return refreshExpiration / 1000;
    }

    /**
     * 토큰에서 memberId 클레임을 추출한다.
     */
    public Long extractMemberId(String token) {
        Claims claims = parseToken(token);
        return claims.get("memberId", Long.class);
    }

    /**
     * 토큰의 서명과 만료 시간을 검증한다.
     * 유효하면 true, 만료·위변조·형식 오류 시 false를 반환한다.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 액세스 토큰을 한 번만 파싱하여 memberId와 role을 함께 추출한다.
     * 인증 필터가 요청마다 사용하므로, 검증·memberId·role을 각각 파싱하지 않고 단일 파싱으로 처리한다.
     * 토큰이 null이거나 만료·위변조·형식 오류이면 빈 Optional을 반환한다.
     * role 클레임이 없는 토큰(구버전 등)이면 role은 null이다.
     */
    public Optional<AccessTokenClaims> parseAccessToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        try {
            Claims claims = parseToken(token);
            return Optional.of(
                    new AccessTokenClaims(claims.get("memberId", Long.class), claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String buildToken(long expiration, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 액세스 토큰에서 인증에 필요한 클레임을 담는 값 객체.
     * role은 클레임이 없으면 null일 수 있다.
     */
    public record AccessTokenClaims(Long memberId, String role) {}
}
