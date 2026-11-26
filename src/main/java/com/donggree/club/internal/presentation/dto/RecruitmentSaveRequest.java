package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.command.RecruitmentSaveCommand;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** 모집 등록·수정·다시 요청 본문. 일시는 시간대를 포함한 ISO 8601로 받아 한국 시간으로 저장한다. */
public record RecruitmentSaveRequest(
        Integer generation,
        boolean alwaysOpen,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean closedEarly,
        List<ClubSaveRequest.Link> links,
        String notice,
        String requesterContact,
        ClubRequesterRole requesterRole) {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    public RecruitmentSaveCommand toCommand() {
        return new RecruitmentSaveCommand(
                generation,
                alwaysOpen,
                korea(startsAt),
                korea(endsAt),
                closedEarly,
                ClubSaveRequest.toLinks(links),
                notice,
                requesterContact,
                requesterRole);
    }

    private static LocalDateTime korea(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KOREA).toLocalDateTime();
    }
}
