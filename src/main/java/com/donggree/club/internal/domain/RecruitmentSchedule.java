package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.time.LocalDateTime;

/**
 * 모집 기간과 마감 여부. 기간 모집은 시작 포함·마감 제외로 열려 있고, 상시모집은 기간 없이 열려 있다.
 * 조기 마감은 기간보다 우선한다. 일시는 한국 시간이다.
 */
public record RecruitmentSchedule(
        boolean alwaysOpen, LocalDateTime startsAt, LocalDateTime endsAt, boolean closedEarly) {

    public RecruitmentSchedule {
        if (alwaysOpen && (startsAt != null || endsAt != null)) {
            throw new IllegalArgumentException("상시모집은 기간을 두지 않습니다");
        }
        if (!alwaysOpen && (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt))) {
            throw new IllegalArgumentException("모집 마감은 시작 이후여야 합니다");
        }
    }

    public RecruitmentStatus statusAt(LocalDateTime now) {
        if (closedEarly) return RecruitmentStatus.CLOSED;
        if (alwaysOpen) return RecruitmentStatus.OPEN;
        if (now.isBefore(startsAt)) return RecruitmentStatus.BEFORE;
        if (!now.isBefore(endsAt)) return RecruitmentStatus.CLOSED;
        return RecruitmentStatus.OPEN;
    }

    /** 지금 마감 일시가 있는 기간 모집이 열려 있으면 그 마감 일시, 아니면 null(상시모집·모집 전·마감). */
    public LocalDateTime openDeadlineAt(LocalDateTime now) {
        return !alwaysOpen && statusAt(now) == RecruitmentStatus.OPEN ? endsAt : null;
    }
}
