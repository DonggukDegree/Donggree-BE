package com.donggree.club.internal.application.admin;

import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 관리자 요청 한 건. 요청 당시 현재 값(before)과 요청 값(after)을 함께 준다.
 *
 * @param before    신규 등록·새 모집이면 null
 * @param imageUrls 변경 전·요청 값의 사진 ID별 주소. 정리돼 없어진 사진은 빠진다
 */
public record AdminClubRequestReadModel(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason,
        ClubRequestPayload before,
        ClubRequestPayload after,
        Requester requester,
        Map<Long, String> imageUrls) {

    /**
     * @param memberId 탈퇴했으면 null
     * @param name     실명. 탈퇴했거나 이름이 없으면 null
     * @param contact  보유 기간이 지나거나 탈퇴했으면 null
     * @param role     보유 기간이 지나거나 탈퇴했으면 null
     */
    public record Requester(Long memberId, String name, String contact, ClubRequesterRole role) {}

    static AdminClubRequestReadModel of(ClubRequest request, String requesterName, Map<Long, String> imageUrls) {
        return new AdminClubRequestReadModel(
                request.getId(),
                request.getType(),
                request.getClubId(),
                request.getClubName(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getRejectReason(),
                request.getBefore(),
                request.getAfter(),
                new Requester(
                        request.getRequesterId(),
                        requesterName,
                        request.getRequesterContact(),
                        request.getRequesterRole()),
                imageUrls);
    }
}
