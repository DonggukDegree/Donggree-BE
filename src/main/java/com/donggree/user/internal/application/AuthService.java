package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.global.auth.MemberAccessGuard;
import com.donggree.user.internal.application.exception.AuthErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 갱신과 로그아웃을 처리하는 응용 서비스.
 * 리프레시 토큰을 검증하고 새 액세스 토큰을 발급하거나, 서버 측 세션을 파기한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final MemberAccessGuard accessGuard;

    /**
     * 리프레시 토큰을 검증하여 새 액세스 토큰을 발급한다.
     * JWT 서명/만료 검증 후, DB에 저장된 토큰과 일치하는지 확인한다.
     *
     * @param refreshToken 클라이언트가 전달한 리프레시 토큰
     * @return 새로 발급한 액세스 토큰
     * @throws GeneralException 리프레시 토큰이 만료·위변조이거나 DB 토큰과 불일치하는 경우
     */
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtTokenProvider.extractMemberId(refreshToken);
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // 정지 회원은 토큰을 다시 받지 못한다. 화면 안내를 위해 정지 종료 시각을 함께 돌려준다.
        var restriction = accessGuard.findRestriction(memberId);
        if (restriction.isPresent()) {
            throw new GeneralException(GeneralErrorCode.MEMBER_SUSPENDED, restriction.get());
        }

        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 리프레시 시 DB의 현재 role을 다시 읽어 발급한다. 권한 변경이 다음 토큰 갱신에 반영된다.
        return jwtTokenProvider.generateAccessToken(memberId, member.getRole().name());
    }

    /**
     * 로그아웃 처리. DB에 저장된 리프레시 토큰을 삭제하여 서버 측 세션을 파기한다.
     *
     * @param memberId 로그아웃할 회원 ID
     */
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        member.clearRefreshToken();
    }
}
