package com.donggree.club.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRecruitmentRepository extends JpaRepository<ClubRecruitment, Long> {

    /** 동아리의 가장 최근 모집 건. */
    Optional<ClubRecruitment> findTopByClubIdOrderByIdDesc(Long clubId);

    /** 이 동아리가 이 기수를 모집한 적이 있는지(기수별 모집 포인트 1회 판단). */
    boolean existsByClubIdAndGeneration(Long clubId, Integer generation);
}
