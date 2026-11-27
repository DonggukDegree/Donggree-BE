package com.donggree.club.internal.application.command;

import java.time.LocalDateTime;

/** 배너 등록·수정 입력. 일시는 한국 시간이다. 입력 규칙은 도메인이 검증한다. */
public record BannerSaveCommand(
        String title,
        String eyebrow,
        String description,
        String background,
        String imageUrl,
        String mobileImageUrl,
        String link,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active) {}
