package com.donggree.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String TEST_SECRET =
            "donggree-test-jwt-secret-key-must-be-at-least-256-bits-long";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties(TEST_SECRET, 1_800_000L, 604_800_000L)
    );

    @Test
    void 액세스_토큰을_생성하고_memberId를_추출한다() {
        String token = jwtTokenProvider.generateAccessToken(1L);

        Long memberId = jwtTokenProvider.extractMemberId(token);

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void 리프레시_토큰을_생성하고_memberId를_추출한다() {
        String token = jwtTokenProvider.generateRefreshToken(42L);

        Long memberId = jwtTokenProvider.extractMemberId(token);

        assertThat(memberId).isEqualTo(42L);
    }

    @Test
    void 유효한_토큰은_검증에_성공한다() {
        String token = jwtTokenProvider.generateAccessToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void 위변조된_토큰은_검증에_실패한다() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                new JwtProperties(TEST_SECRET, 1L, 1L)
        );

        String token = shortLivedProvider.generateAccessToken(1L);
        Thread.sleep(5);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    void null_토큰은_검증에_실패한다() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
