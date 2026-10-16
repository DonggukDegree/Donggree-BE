package com.donggree.curriculum.internal.presentation.dto.organization;

import com.donggree.curriculum.internal.application.readmodel.organization.CollegeReadModel;

/** 단과대학 조회 응답. */
public record CollegeResponse(Long id, String collegeName) {
    public static CollegeResponse from(CollegeReadModel p) {
        return new CollegeResponse(p.id(), p.collegeName());
    }
}
