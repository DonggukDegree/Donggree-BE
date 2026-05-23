package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 토큰 갱신을 처리하는 응용 서비스.
 * 리프레시 토큰을 검증하고 새 액세스 토큰을 발급한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 리프레시 토큰을 검증하여 새 액세스 토큰을 발급한다.
     *
     * @param refreshToken 클라이언트가 전달한 리프레시 토큰
     * @return 새로 발급한 액세스 토큰
     * @throws GeneralException 리프레시 토큰이 만료·위변조·형식 오류인 경우
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtTokenProvider.extractMemberId(refreshToken);
        return jwtTokenProvider.generateAccessToken(memberId);
    }
}
