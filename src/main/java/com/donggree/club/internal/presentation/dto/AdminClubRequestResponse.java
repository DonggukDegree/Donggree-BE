package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.admin.AdminClubRequestReadModel;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDateTime;

/**
 * 관리자 요청 한 건.
 *
 * @param before 요청 당시 현재 값({@link ClubRequestValue}). 신규 등록·새 모집이면 null
 * @param after  요청 값({@link ClubRequestValue}). 요청자 연락처·관계는 requester에만 담는다
 */
public record AdminClubRequestResponse(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason,
        Object before,
        Object after,
        Requester requester) {

    public record Requester(Long memberId, String name, String contact, ClubRequesterRole role) {}

    public static AdminClubRequestResponse from(AdminClubRequestReadModel request) {
        var requester = request.requester();
        return new AdminClubRequestResponse(
                request.id(),
                request.type(),
                request.clubId(),
                request.clubName(),
                request.status(),
                request.requestedAt(),
                request.processedAt(),
                request.rejectReason(),
                ClubRequestValue.of(request.before(), null, null),
                ClubRequestValue.of(request.after(), null, null),
                new Requester(requester.memberId(), requester.name(), requester.contact(), requester.role()));
    }
}
