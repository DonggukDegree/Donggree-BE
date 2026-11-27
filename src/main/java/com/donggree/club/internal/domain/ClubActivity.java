package com.donggree.club.internal.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;

/**
 * 현재 활동 확인 여부.
 *
 * <p>기수제 동아리는 오늘이 현재 기수 활동 기간 안이면 활동 중으로 본다. 기수 없이 활동하는 동아리는 매년 3월 1일·9월 1일을
 * 확인 시점으로 두고, 가장 최근 확인 시점 이후에 정보가 등록·수정됐으면 활동 중으로 본다.
 *
 * @param infoUpdatedAt 마지막 정보 등록·수정 시각(한국 시간)
 */
public record ClubActivity(
        Integer generation, LocalDate activityStartDate, LocalDate activityEndDate, LocalDateTime infoUpdatedAt) {

    private static final MonthDay SPRING_CHECK = MonthDay.of(3, 1);
    private static final MonthDay FALL_CHECK = MonthDay.of(9, 1);

    public boolean confirmedOn(LocalDate today) {
        if (generation != null) {
            return !today.isBefore(activityStartDate) && !today.isAfter(activityEndDate);
        }
        return !infoUpdatedAt.toLocalDate().isBefore(latestCheckOn(today));
    }

    static LocalDate latestCheckOn(LocalDate today) {
        LocalDate fall = FALL_CHECK.atYear(today.getYear());
        if (!today.isBefore(fall)) return fall;
        LocalDate spring = SPRING_CHECK.atYear(today.getYear());
        if (!today.isBefore(spring)) return spring;
        return FALL_CHECK.atYear(today.getYear() - 1);
    }
}
