package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.MyClubRequestReadModel;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import java.time.LocalDateTime;

public record MyClubRequestResponse(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason) {

    public static MyClubRequestResponse from(MyClubRequestReadModel request) {
        return new MyClubRequestResponse(
                request.id(),
                request.type(),
                request.clubId(),
                request.clubName(),
                request.status(),
                request.requestedAt(),
                request.processedAt(),
                request.rejectReason());
    }
}
