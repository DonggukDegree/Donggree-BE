package com.donggree.user;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 관리자 신고 처리·회원 관리용 공개 인터페이스. place 모듈의 통합 신고 대시보드가 사용한다.
 * 정지는 동그리 전체 이용 제한이며, 로그인·토큰 재발급·API 요청을 모두 막는다.
 */
public interface UserModerationService {

    /** 관리자 화면용 회원 정보(탈퇴 회원 제외). null ID는 무시한다. */
    Map<Long, MemberAdminProfile> findAdminProfiles(Collection<Long> memberIds);

    /** 학번·이름·닉네임에 keyword가 들어간 회원을 가입 최신순으로 찾는다. keyword가 비면 전체. */
    MemberSearchPage searchMembers(String keyword, int page, int size);

    /**
     * 회원을 정지한다. 지금부터 days일 동안 이용할 수 없고, 정지 중이면 새 일수로 바꾼다. 기존 로그인도 끊는다.
     *
     * @throws com.donggree.global.apiPayload.exception.GeneralException 없는 회원(404), 잘못된 일수(400)
     */
    MemberSuspensionRecord suspend(SuspendMemberCommand command);

    /** 정지를 바로 해제한다. 정지 중이 아니면 아무것도 바꾸지 않는다. */
    void lift(Long memberId, Long adminId);

    /** 정지 이력(최신순). */
    List<MemberSuspensionRecord> findSuspensions(Long memberId);

    /** 신고 처리로 정지된 횟수를 신고 종류별로 센다(회원별 누적 신고 집계용). */
    Map<Long, Long> countReportSuspensions(Collection<Long> memberIds, ReportTargetType reportType);
}
