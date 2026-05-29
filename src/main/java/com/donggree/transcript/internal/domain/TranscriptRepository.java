package com.donggree.transcript.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    Optional<Transcript> findByMemberId(Long memberId);

    @Query("SELECT t FROM Transcript t LEFT JOIN FETCH t.courseRecords WHERE t.id = :id")
    Optional<Transcript> findWithCourseRecordsById(@Param("id") Long id);
}
