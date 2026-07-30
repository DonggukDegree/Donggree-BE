package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.projection.CollegeProjection;

/** 단과대학 조회 응답. */
public record CollegeResponse(Long id, String collegeName) {
    public static CollegeResponse from(CollegeProjection p) {
        return new CollegeResponse(p.id(), p.collegeName());
    }
}
