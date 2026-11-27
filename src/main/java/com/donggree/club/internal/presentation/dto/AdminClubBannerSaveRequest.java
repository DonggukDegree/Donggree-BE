package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.command.BannerSaveCommand;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 배너 등록·수정 본문. 노출 기간은 시간대를 포함한 ISO 8601로 받아 한국 시간으로 저장한다. */
public record AdminClubBannerSaveRequest(
        String title,
        String eyebrow,
        String description,
        String background,
        String imageUrl,
        String mobileImageUrl,
        String link,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean active) {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    public BannerSaveCommand toCommand() {
        return new BannerSaveCommand(
                title,
                eyebrow,
                description,
                background,
                imageUrl,
                mobileImageUrl,
                link,
                korea(startsAt),
                korea(endsAt),
                active);
    }

    private static LocalDateTime korea(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KOREA).toLocalDateTime();
    }
}
