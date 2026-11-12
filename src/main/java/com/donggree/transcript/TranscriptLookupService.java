package com.donggree.transcript;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 다른 모듈에서 성적표 데이터를 조회하기 위한 공개 인터페이스.
 * graduation 모듈이 졸업 판정에 필요한 transcript + course_record를 함께 조회할 때 사용한다.
 */
public interface TranscriptLookupService {

    /** transcriptId로 수강 이력을 포함한 성적표를 조회한다. 삭제된 성적표는 반환하지 않는다. */
    Optional<TranscriptView> findById(Long transcriptId);

    /** memberId로 수강 이력을 포함한 성적표를 조회한다. 삭제된 성적표는 반환하지 않는다. */
    Optional<TranscriptView> findByMemberId(Long memberId);

    /**
     * 회원별 성적표의 주전공 학과 ID. 관리자 신고 대시보드의 회원 행에 단과대·학과를 보여줄 때 쓴다.
     * 성적표가 없거나 학과가 연결되지 않은 회원은 결과에 넣지 않는다. 수강 이력은 읽지 않는다.
     */
    Map<Long, Long> findDepartmentIdsByMemberIds(Collection<Long> memberIds);
}
