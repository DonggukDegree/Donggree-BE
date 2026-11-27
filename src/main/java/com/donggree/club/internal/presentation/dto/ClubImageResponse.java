package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubImageReadModel;

/** @param url API 서버 기준 사진 경로(/api/clubs/images/...) */
public record ClubImageResponse(Long id, String url) {

    public static ClubImageResponse from(ClubImageReadModel image) {
        return new ClubImageResponse(image.id(), image.url());
    }
}
