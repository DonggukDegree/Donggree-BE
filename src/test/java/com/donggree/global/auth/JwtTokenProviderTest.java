package com.donggree.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "donggree-test-jwt-secret-key-must-be-at-least-256-bits-long";

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(new JwtProperties(TEST_SECRET, 1_800_000L, 604_800_000L));

    @Test
    void 액세스_토큰을_생성하고_memberId를_추출한다() {
        String token = jwtTokenProvider.generateAccessToken(1L, "STUDENT");

        Long memberId = jwtTokenProvider.extractMemberId(token);

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void 액세스_토큰을_단일_파싱하여_memberId와_role을_함께_추출한다() {
        String token = jwtTokenProvider.generateAccessToken(7L, "SUPER_ADMIN");

        var claims = jwtTokenProvider.parseAccessToken(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().memberId()).isEqualTo(7L);
        assertThat(claims.get().role()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void role_클레임이_없는_토큰은_파싱_결과_role이_null이다() {
        // 리프레시 토큰은 role 클레임이 없어, role 없는 구버전 액세스 토큰과 구조가 동일하다.
        String tokenWithoutRole = jwtTokenProvider.generateRefreshToken(1L);

        var claims = jwtTokenProvider.parseAccessToken(tokenWithoutRole);

        assertThat(claims).isPresent();
        assertThat(claims.get().memberId()).isEqualTo(1L);
        assertThat(claims.get().role()).isNull();
    }

    @Test
    void null이거나_위변조된_토큰은_파싱_결과가_비어있다() {
        assertThat(jwtTokenProvider.parseAccessToken(null)).isEmpty();
        assertThat(jwtTokenProvider.parseAccessToken("invalid.token.value")).isEmpty();
    }

    @Test
    void 리프레시_토큰을_생성하고_memberId를_추출한다() {
        String token = jwtTokenProvider.generateRefreshToken(42L);

        Long memberId = jwtTokenProvider.extractMemberId(token);

        assertThat(memberId).isEqualTo(42L);
    }

    @Test
    void 유효한_토큰은_검증에_성공한다() {
        String token = jwtTokenProvider.generateAccessToken(1L, "STUDENT");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void 위변조된_토큰은_검증에_실패한다() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(new JwtProperties(TEST_SECRET, 1L, 1L));

        String token = shortLivedProvider.generateAccessToken(1L, "STUDENT");
        Thread.sleep(5);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    void null_토큰은_검증에_실패한다() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
