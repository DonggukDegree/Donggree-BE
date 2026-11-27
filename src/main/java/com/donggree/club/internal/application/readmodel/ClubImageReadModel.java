package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubImage;

/** 올린 사진. url은 API 서버 기준 경로다. */
public record ClubImageReadModel(Long id, String url) {

    public static ClubImageReadModel from(ClubImage image) {
        return new ClubImageReadModel(image.getId(), image.url());
    }
}
