package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.global.auth.MemberAccessGuard;
import com.donggree.user.internal.application.exception.AuthErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthServiceTest {

    private static final String TEST_SECRET = "donggree-test-jwt-secret-key-must-be-at-least-256-bits-long";

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(new JwtProperties(TEST_SECRET, 1_800_000L, 604_800_000L));

    private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);

    private final MemberAccessGuard accessGuard = Mockito.mock(MemberAccessGuard.class);

    private final AuthService authService = new AuthService(jwtTokenProvider, memberRepository, accessGuard);

    @Test
    void 유효한_리프레시_토큰으로_새_액세스_토큰을_발급한다() {
        Long memberId = 1L;
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        Member member = Member.registerKakaoMember("kakao-123", "test@example.com");
        member.updateRefreshToken(refreshToken);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        String accessToken = authService.refreshAccessToken(refreshToken);

        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.extractMemberId(accessToken)).isEqualTo(memberId);
    }

    @Test
    void DB에_저장된_토큰과_불일치하면_예외를_던진다() {
        Long memberId = 1L;
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        Member member = Member.registerKakaoMember("kakao-123", "test@example.com");
        member.updateRefreshToken("different-token-in-db");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex ->
                        assertThat(((GeneralException) ex).getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void 존재하지_않는_회원이면_예외를_던진다() {
        Long memberId = 999L;
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex ->
                        assertThat(((GeneralException) ex).getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void 위변조된_리프레시_토큰이면_예외를_던진다() {
        assertThatThrownBy(() -> authService.refreshAccessToken("invalid.token.value"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex ->
                        assertThat(((GeneralException) ex).getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void 만료된_리프레시_토큰이면_예외를_던진다() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(new JwtProperties(TEST_SECRET, -60_000L, -60_000L));
        AuthService shortLivedAuthService = new AuthService(shortLivedProvider, memberRepository, accessGuard);

        String refreshToken = shortLivedProvider.generateRefreshToken(1L);

        assertThatThrownBy(() -> shortLivedAuthService.refreshAccessToken(refreshToken))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex ->
                        assertThat(((GeneralException) ex).getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void null_리프레시_토큰이면_예외를_던진다() {
        assertThatThrownBy(() -> authService.refreshAccessToken(null))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex ->
                        assertThat(((GeneralException) ex).getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void 로그아웃하면_리프레시_토큰이_삭제된다() {
        Long memberId = 1L;
        Member member = Member.registerKakaoMember("kakao-123", "test@example.com");
        member.updateRefreshToken("stored-refresh-token");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        authService.logout(memberId);

        assertThat(member.getRefreshToken()).isNull();
    }

    @Test
    void 정지_회원은_토큰을_다시_받지_못하고_정지_내용을_함께_받는다() {
        Long memberId = 1L;
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);
        Member member = Member.registerKakaoMember("oauth-suspended", "suspended@example.invalid");
        member.updateRefreshToken(refreshToken);
        Mockito.when(memberRepository.findById(memberId)).thenReturn(java.util.Optional.of(member));
        var restriction = new com.donggree.global.auth.AccessRestriction(
                java.time.LocalDateTime.of(2026, 12, 13, 10, 0), 30, true);
        Mockito.when(accessGuard.findRestriction(memberId)).thenReturn(java.util.Optional.of(restriction));

        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    assertThat(((GeneralException) e).getCode())
                            .isEqualTo(com.donggree.global.apiPayload.code.GeneralErrorCode.MEMBER_SUSPENDED);
                    assertThat(((GeneralException) e).getResult()).isEqualTo(restriction);
                });
    }
}
