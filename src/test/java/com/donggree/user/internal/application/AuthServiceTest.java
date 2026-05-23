package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final String TEST_SECRET =
            "donggree-test-jwt-secret-key-must-be-at-least-256-bits-long";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties(TEST_SECRET, 1_800_000L, 604_800_000L)
    );

    private final AuthService authService = new AuthService(jwtTokenProvider);

    @Test
    void 유효한_리프레시_토큰으로_새_액세스_토큰을_발급한다() {
        Long memberId = 1L;
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        String accessToken = authService.refreshAccessToken(refreshToken);

        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.extractMemberId(accessToken)).isEqualTo(memberId);
    }

    @Test
    void 위변조된_리프레시_토큰이면_예외를_던진다() {
        assertThatThrownBy(() -> authService.refreshAccessToken("invalid.token.value"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void 만료된_리프레시_토큰이면_예외를_던진다() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                new JwtProperties(TEST_SECRET, 1L, 1L)
        );
        AuthService shortLivedAuthService = new AuthService(shortLivedProvider);

        String refreshToken = shortLivedProvider.generateRefreshToken(1L);
        Thread.sleep(5);

        assertThatThrownBy(() -> shortLivedAuthService.refreshAccessToken(refreshToken))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void null_리프레시_토큰이면_예외를_던진다() {
        assertThatThrownBy(() -> authService.refreshAccessToken(null))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}
