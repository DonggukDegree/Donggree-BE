package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 화면에 보여줄 현재 모집과 지난 모집.
 *
 * <p>가장 최근 모집 건이 현재 모집이다. 다만 그 모집이 마감됐고 현재 기수 활동 기간도 끝났으면 다음 모집을 기다리는 중이라
 * 현재 모집은 없음(모집 전)으로 두고, 그 건을 지난 모집으로 보여준다. 기수 없이 활동하는 동아리는 활동 기간이 없어 마감 상태를 유지한다.
 *
 * @param current  현재 모집. 없으면 null
 * @param previous 현재 모집이 없을 때 보여줄 가장 최근의 지난 모집. 없으면 null
 */
public record RecruitmentSelection<T>(T current, T previous) {

    public static <T> RecruitmentSelection<T> of(
            T latest, RecruitmentSchedule latestSchedule, LocalDate activityEndDate, LocalDateTime now) {
        if (latest == null) return new RecruitmentSelection<>(null, null);
        boolean waitingNext = latestSchedule.statusAt(now) == RecruitmentStatus.CLOSED
                && activityEndDate != null
                && now.toLocalDate().isAfter(activityEndDate);
        return waitingNext ? new RecruitmentSelection<>(null, latest) : new RecruitmentSelection<>(latest, null);
    }
}
