package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.projection.AreaTypeProjection;

/** 이수 영역 조회 응답. */
public record AreaTypeResponse(Long id, String areaName) {
    public static AreaTypeResponse from(AreaTypeProjection p) {
        return new AreaTypeResponse(p.id(), p.areaName());
    }
}
