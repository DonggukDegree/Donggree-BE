package com.donggree.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 사용하여 로그인한 회원의 ID를 주입받는다.
 * SecurityContext의 Authentication principal에서 memberId를 추출한다.
 *
 * <pre>
 * {@code @PostMapping("/logout")
 * public ApiResponse<Void> logout(@LoginMemberId Long memberId) { ... }}
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMemberId {
}
