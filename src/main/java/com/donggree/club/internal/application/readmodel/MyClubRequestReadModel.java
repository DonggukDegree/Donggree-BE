package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import java.time.LocalDateTime;

/**
 * 내 요청 내역 한 건.
 *
 * @param clubId      대상 동아리. 승인 전 신규 등록은 null
 * @param requestedAt 요청 시각
 * @param processedAt 승인·반려·취소 시각. 검토 중이면 null
 */
public record MyClubRequestReadModel(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason) {

    public static MyClubRequestReadModel from(ClubRequest request) {
        return new MyClubRequestReadModel(
                request.getId(),
                request.getType(),
                request.getClubId(),
                request.getClubName(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getRejectReason());
    }
}
