package com.donggree.global.auth;

import java.time.LocalDateTime;

/**
 * 화면 안내에 쓰는 정지 내용. 정지 회원의 로그인·토큰 재발급·API 응답 result로 내려간다.
 *
 * @param suspendedUntil 이 시각부터 다시 이용 가능
 * @param days           정지 일수
 * @param byReport       신고 처리로 인한 정지인지(아니면 관리자 판단)
 */
@org.springframework.modulith.NamedInterface("auth")
public record AccessRestriction(LocalDateTime suspendedUntil, int days, boolean byReport) {}
