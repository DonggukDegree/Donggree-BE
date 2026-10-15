package com.donggree.curriculum.internal.presentation.dto.organization;

import com.donggree.curriculum.internal.application.projection.organization.DepartmentProjection;

/** 학과 조회 응답. */
public record DepartmentResponse(Long id, Long collegeId, String collegeName, String departmentName) {
    public static DepartmentResponse from(DepartmentProjection p) {
        return new DepartmentResponse(p.id(), p.collegeId(), p.collegeName(), p.departmentName());
    }
}
