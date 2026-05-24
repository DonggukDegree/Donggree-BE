package com.donggree.curriculum.internal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSetRepository extends JpaRepository<RequirementSet, Long> {

    List<RequirementSet> findByDepartmentIdAndActiveTrue(Long departmentId);
}
