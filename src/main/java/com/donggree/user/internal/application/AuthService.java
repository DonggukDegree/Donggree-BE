package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.JwtTokenProvider;
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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return jwtTokenProvider.generateAccessToken(memberId);
    }

    /**
     * 로그아웃 처리. DB에 저장된 리프레시 토큰을 삭제하여 서버 측 세션을 파기한다.
     *
     * @param memberId 로그아웃할 회원 ID
     */
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        member.clearRefreshToken();
    }
}
