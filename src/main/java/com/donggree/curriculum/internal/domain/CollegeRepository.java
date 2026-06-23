package com.donggree.curriculum.internal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {

    Optional<College> findByCollegeName(String collegeName);

    /** 드롭다운용: 전체 단과대 목록을 이름순으로 조회한다. */
    List<College> findAllByOrderByCollegeNameAsc();
}
