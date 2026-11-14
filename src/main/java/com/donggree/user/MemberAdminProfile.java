package com.donggree.user;

import java.time.LocalDateTime;

/**
 * 관리자 화면용 회원 정보. 신고 대시보드의 회원 행에서 쓰며 일반 회원 화면에는 내보내지 않는다.
 *
 * @param suspendedUntil 정지 중이면 종료 시각, 아니면 null
 */
public record MemberAdminProfile(
        Long memberId,
        String nickname,
        String name,
        String studentId,
        String role,
        LocalDateTime joinedAt,
        LocalDateTime suspendedUntil) {}
