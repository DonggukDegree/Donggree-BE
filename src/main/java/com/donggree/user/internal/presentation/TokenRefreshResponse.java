package com.donggree.user.internal.presentation;

/**
 * 토큰 갱신 응답 DTO.
 *
 * @param accessToken 새로 발급된 액세스 토큰
 */
public record TokenRefreshResponse(String accessToken) {
}
