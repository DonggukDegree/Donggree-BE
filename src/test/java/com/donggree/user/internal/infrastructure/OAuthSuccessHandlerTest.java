package com.donggree.user.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.auth.AccessRestriction;
import com.donggree.global.auth.JwtProperties;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.global.auth.MemberAccessGuard;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

class OAuthSuccessHandlerTest {

    private static final String SECRET = "donggree-test-jwt-secret-key-must-be-at-least-256-bits-long";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberAccessGuard accessGuard = mock(MemberAccessGuard.class);
    private final OAuthSuccessHandler handler = new OAuthSuccessHandler(
            new JwtTokenProvider(new JwtProperties(SECRET, 1_800_000L, 604_800_000L)), memberRepository, accessGuard);
    private Member member;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", "http://localhost:5173/login/callback");
        member = Member.registerKakaoMember("kakao-1", "a@example.invalid");
        ReflectionTestUtils.setField(member, "id", 7L);
    }

    @Test
    void 정지_회원은_토큰_없이_정지_내용만_로그인_콜백으로_넘긴다() throws Exception {
        given(accessGuard.findRestriction(7L))
                .willReturn(Optional.of(new AccessRestriction(LocalDateTime.of(2026, 12, 13, 10, 0), 30, true)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication());

        assertThat(response.getRedirectedUrl())
                .startsWith("http://localhost:5173/login/callback?")
                .contains("suspendedUntil=2026-12-13T10:00")
                .contains("suspensionDays=30")
                .contains("suspensionByReport=true")
                .doesNotContain("accessToken");
        assertThat(response.getHeader("Set-Cookie")).isNull();
        verify(memberRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void 정지가_아니면_기존처럼_토큰을_발급한다() throws Exception {
        given(accessGuard.findRestriction(7L)).willReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication());

        assertThat(response.getRedirectedUrl()).contains("accessToken=");
        assertThat(response.getHeader("Set-Cookie")).startsWith("refreshToken=");
    }

    private TestingAuthenticationToken authentication() {
        return new TestingAuthenticationToken(new OAuthMember(member, Map.of("id", "kakao-1")), null);
    }
}
