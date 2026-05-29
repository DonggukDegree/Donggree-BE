package com.donggree.user.event;

/**
 * 회원 탈퇴 시 발행되는 도메인 이벤트.
 * 다른 모듈에서 탈퇴 회원의 관련 데이터를 정리할 때 사용한다.
 *
 * @param memberId 탈퇴한 회원 ID
 */
public record MemberWithdrawnEvent(Long memberId) {}
