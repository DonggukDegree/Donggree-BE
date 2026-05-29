package com.donggree.global.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 토큰 생성/검증에 필요한 설정값을 바인딩하는 프로퍼티 클래스.
 *
 * @param secret          HMAC-SHA256 서명에 사용할 비밀키 (최소 256비트)
 * @param accessExpiration  액세스 토큰 만료 시간 (밀리초)
 * @param refreshExpiration 리프레시 토큰 만료 시간 (밀리초)
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(@NotBlank String secret, long accessExpiration, long refreshExpiration) {}
