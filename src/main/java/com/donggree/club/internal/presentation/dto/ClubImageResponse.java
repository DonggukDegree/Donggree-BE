package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubImageReadModel;

/** @param url 공개 주소 설정 기준 사진 주소(디스크 저장은 /api/clubs/images/..., R2 저장은 https 공개 주소) */
public record ClubImageResponse(Long id, String url) {

    public static ClubImageResponse from(ClubImageReadModel image) {
        return new ClubImageResponse(image.id(), image.url());
    }
}
