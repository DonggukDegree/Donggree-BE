package com.donggree.global.auth;

import java.util.Optional;

/**
 * 인증된 회원의 이용 제한(정지) 여부를 확인하는 포트. global은 다른 모듈에 의존하지 않으므로
 * 구현은 user 모듈이 맡고, JWT 필터·로그인·토큰 재발급이 이 포트로 차단 여부를 묻는다.
 */
@org.springframework.modulith.NamedInterface("auth")
public interface MemberAccessGuard {

    /** 지금 정지 중이면 제한 내용을, 아니면 빈 값을 돌려준다. */
    Optional<AccessRestriction> findRestriction(Long memberId);
}
