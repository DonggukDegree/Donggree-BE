package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubBannerReadModel;

/** 공개 배너. 이미지가 있으면 이미지 배너(title은 대체 텍스트), 없으면 배경 + 글자 배너다. */
public record ClubBannerResponse(
        Long id,
        String title,
        String eyebrow,
        String description,
        String imageUrl,
        String mobileImageUrl,
        String background,
        String link) {

    public static ClubBannerResponse from(ClubBannerReadModel banner) {
        return new ClubBannerResponse(
                banner.id(),
                banner.title(),
                banner.eyebrow(),
                banner.description(),
                banner.imageUrl(),
                banner.mobileImageUrl(),
                banner.background(),
                banner.link());
    }
}
