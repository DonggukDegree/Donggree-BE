package com.donggree.place.internal.application.admin;

import java.time.LocalDateTime;

/**
 * 신고 대시보드의 회원 행. 이름·학번은 회원 정보, 단과대·학과는 회원의 성적표 주전공에서 가져온다
 * (성적표가 없으면 null). 탈퇴했거나 연결이 끊긴 회원은 행 대신 null로 내려간다.
 *
 * @param suspendedUntil 정지 중이면 종료 시각
 */
public record AdminMemberSummary(
        Long memberId,
        String nickname,
        String name,
        String studentId,
        String collegeName,
        String departmentName,
        LocalDateTime suspendedUntil) {}
