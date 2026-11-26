package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.command.ClubSaveResult;

/** @param applied DIRECT(바로 반영)·PENDING(승인 요청) */
public record ClubSaveResponse(String applied, Long clubId) {

    public static ClubSaveResponse from(ClubSaveResult result) {
        return new ClubSaveResponse(result.applied().name(), result.clubId());
    }
}
