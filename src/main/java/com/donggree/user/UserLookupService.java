package com.donggree.user;

/**
 * 다른 모듈에서 회원 상태를 조회하기 위한 공개 인터페이스.
 * place 모듈이 장소 등록 등 쓰기 권한을 판단할 때 사용한다.
 */
public interface UserLookupService {

    /**
     * PDF 본인인증 또는 학교 이메일 인증을 완료한 학생인지 확인한다.
     * 회원이 없거나 탈퇴한 경우 false를 반환한다.
     *
     * @param memberId 확인할 회원 ID
     */
    boolean isStudentVerified(Long memberId);
}
