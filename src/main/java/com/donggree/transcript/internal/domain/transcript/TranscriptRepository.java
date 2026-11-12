package com.donggree.transcript.internal.domain.transcript;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    Optional<Transcript> findByMemberId(Long memberId);

    /** 회원별 주전공 학과 ID만 읽는다(삭제된 성적표·학과 미연결 제외). */
    @Query("select t.memberId, t.departmentId from Transcript t"
            + " where t.memberId in :memberIds and t.departmentId is not null")
    java.util.List<Object[]> findDepartmentIdsByMemberIds(@Param("memberIds") java.util.Collection<Long> memberIds);

    @Query("SELECT t FROM Transcript t LEFT JOIN FETCH t.courseRecords WHERE t.id = :id")
    Optional<Transcript> findWithCourseRecordsById(@Param("id") Long id);

    @Query("SELECT t FROM Transcript t LEFT JOIN FETCH t.courseRecords WHERE t.memberId = :memberId")
    Optional<Transcript> findWithCourseRecordsByMemberId(@Param("memberId") Long memberId);
}
