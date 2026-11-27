package com.donggree.club.internal.application.command;

import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 모집 등록·수정 입력. 일시는 한국 시간이다.
 *
 * @param requesterContact 승인 필요 요청의 확인 연락처. 바로 반영이면 무시
 * @param requesterRole    승인 필요 요청의 동아리와의 관계. 바로 반영이면 무시
 */
public record RecruitmentSaveCommand(
        Integer generation,
        boolean alwaysOpen,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean closedEarly,
        List<ClubSaveCommand.Link> links,
        String notice,
        String requesterContact,
        ClubRequesterRole requesterRole) {}
