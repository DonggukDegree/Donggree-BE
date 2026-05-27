package com.donggree.user;

/**
 * PDF 업로드 시 회원 본인 확인을 위한 공개 인터페이스.
 * transcript 모듈에서 호출한다.
 */
public interface MemberIdentityService {

    /**
     * PDF에서 파싱한 학번·이름이 회원 정보와 일치하는지 검증한다.
     * 회원이 아직 온보딩(학번 등록)을 완료하지 않은 경우 검증을 건너뛴다.
     * 불일치 시 {@link com.donggree.global.apiPayload.exception.GeneralException}을 던진다.
     *
     * @param memberId     업로드 요청 회원 ID
     * @param pdfStudentId PDF에서 파싱한 학번 (null이면 검증 건너뜀)
     * @param pdfName      PDF에서 파싱한 성명 (null이면 검증 건너뜀)
     */
    void validatePdfOwner(Long memberId, String pdfStudentId, String pdfName);

    /**
     * PDF 학번·이름이 회원 정보와 일치하면 본인 인증 완료 처리한다.
     * 온보딩 미완료이거나 불일치이면 아무 작업도 하지 않는다.
     *
     * @param memberId     업로드 요청 회원 ID
     * @param pdfStudentId PDF에서 파싱한 학번
     * @param pdfName      PDF에서 파싱한 성명
     */
    void verifyIdentityIfMatch(Long memberId, String pdfStudentId, String pdfName);
}
