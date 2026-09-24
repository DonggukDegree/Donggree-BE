package com.donggree.curriculum.internal.domain.department;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCollegeIdAndDepartmentName(Long collegeId, String departmentName);

    /** 단과대 이동으로 같은 학과명이 여러 개일 수 있다. */
    List<Department> findAllByDepartmentName(String departmentName);

    /** 단과대에 속한 학과를 조회한다(필터 해석용). */
    List<Department> findByCollegeId(Long collegeId);

    /** 드롭다운용: 단과대로 필터링한 학과 목록을 학과명순으로 조회한다. */
    List<Department> findByCollegeIdOrderByDepartmentNameAsc(Long collegeId);

    /** 드롭다운용: 전체 학과 목록을 학과명순으로 조회한다. */
    List<Department> findAllByOrderByDepartmentNameAsc();
}
