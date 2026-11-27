package com.donggree.club.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRecruitmentRepository extends JpaRepository<ClubRecruitment, Long> {

    /** 동아리의 가장 최근 모집 건. */
    Optional<ClubRecruitment> findTopByClubIdOrderByIdDesc(Long clubId);
}
