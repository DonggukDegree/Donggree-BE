package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.MyClubRequestDetailReadModel;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import java.time.LocalDateTime;

/**
 * 내 요청 한 건과 보냈던 값.
 *
 * @param after 보냈던 값({@link ClubRequestValue}). 보냈던 연락처·관계를 함께 담는다
 */
public record MyClubRequestDetailResponse(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason,
        Object after) {

    public static MyClubRequestDetailResponse from(MyClubRequestDetailReadModel detail) {
        var request = detail.request();
        return new MyClubRequestDetailResponse(
                request.id(),
                request.type(),
                request.clubId(),
                request.clubName(),
                request.status(),
                request.requestedAt(),
                request.processedAt(),
                request.rejectReason(),
                ClubRequestValue.of(
                        detail.after(), detail.requesterContact(), detail.requesterRole(), detail.imageUrls()));
    }
}
