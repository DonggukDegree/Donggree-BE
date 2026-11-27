package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubImage;

/** 올린 사진. url은 공개 주소 설정 기준 주소다. */
public record ClubImageReadModel(Long id, String url) {

    public static ClubImageReadModel from(ClubImage image, String url) {
        return new ClubImageReadModel(image.getId(), url);
    }
}
