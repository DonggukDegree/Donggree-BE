package com.donggree.user;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 모듈에서 회원 상태를 조회하기 위한 공개 인터페이스.
 * place 모듈이 장소 등록 등 쓰기 권한을 판단하고, 등록자·작성자 닉네임을 표시할 때 사용한다.
 */
public interface UserLookupService {

    /**
     * PDF 본인인증 또는 학교 이메일 인증을 완료한 학생인지 확인한다.
     * 회원이 없거나 탈퇴한 경우 false를 반환한다.
     *
     * @param memberId 확인할 회원 ID
     */
    boolean isStudentVerified(Long memberId);

    /**
     * 회원 표시 정보를 한 번에 조회한다. 탈퇴했거나 없는 회원은 결과에 넣지 않으며,
     * 호출 측은 빠진 회원을 "탈퇴한 회원"으로 표시한다. null ID는 무시한다.
     *
     * @param memberIds 조회할 회원 ID 목록
     * @return 회원 ID별 표시 정보
     */
    Map<Long, MemberProfile> findProfiles(Collection<Long> memberIds);
}
