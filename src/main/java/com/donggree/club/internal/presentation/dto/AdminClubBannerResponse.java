package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubBannerReadModel;
import java.time.LocalDateTime;

/** 관리자 배너. 공개 배너에 노출 켜짐·기간·순서를 더한다. */
public record AdminClubBannerResponse(
        Long id,
        String title,
        String eyebrow,
        String description,
        String imageUrl,
        String mobileImageUrl,
        String background,
        String link,
        boolean active,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int order) {

    public static AdminClubBannerResponse from(ClubBannerReadModel banner) {
        return new AdminClubBannerResponse(
                banner.id(),
                banner.title(),
                banner.eyebrow(),
                banner.description(),
                banner.imageUrl(),
                banner.mobileImageUrl(),
                banner.background(),
                banner.link(),
                banner.active(),
                banner.startsAt(),
                banner.endsAt(),
                banner.order());
    }
}
