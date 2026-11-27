package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubBanner;
import java.time.LocalDateTime;

/** 배너 한 개. 공개 목록은 노출 조건 필드를 쓰지 않는다. */
public record ClubBannerReadModel(
        Long id,
        String title,
        String eyebrow,
        String description,
        String background,
        String imageUrl,
        String mobileImageUrl,
        String link,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        int order) {

    public static ClubBannerReadModel from(ClubBanner banner) {
        return new ClubBannerReadModel(
                banner.getId(),
                banner.getTitle(),
                banner.getEyebrow(),
                banner.getDescription(),
                banner.getBackground(),
                banner.getImageUrl(),
                banner.getMobileImageUrl(),
                banner.getLink(),
                banner.getStartsAt(),
                banner.getEndsAt(),
                banner.isActive(),
                banner.getSortOrder());
    }
}
