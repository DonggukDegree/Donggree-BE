package com.donggree.user;

/**
 * 회원 정지 명령. 정지 일수는 1일 이상(업무 상한 없음). 정지 중이면 지금부터 새 일수로 바꾼다.
 *
 * @param reportType 신고 처리로 정지할 때 근거 신고 종류. 관리자 판단 정지면 null
 * @param reportId   근거 신고 ID. 관리자 판단 정지면 null
 * @param note       관리자 메모(최대 200자, 선택)
 * @param adminId    처리 관리자 회원 ID
 */
public record SuspendMemberCommand(
        Long memberId,
        int days,
        SuspensionSource source,
        ReportTargetType reportType,
        Long reportId,
        String note,
        Long adminId) {}
