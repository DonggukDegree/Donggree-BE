package com.donggree.transcript.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    Optional<Transcript> findByMemberId(Long memberId);
}
