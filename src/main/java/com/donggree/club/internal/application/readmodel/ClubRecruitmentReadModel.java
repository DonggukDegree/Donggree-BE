package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubLink;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.RecruitmentSchedule;
import java.time.LocalDateTime;
import java.util.List;

/** 모집 건 한 개. 일시는 한국 시간이다. */
public record ClubRecruitmentReadModel(
        Long id,
        Integer generation,
        boolean alwaysOpen,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean closedEarly,
        List<ClubLink> links,
        String notice) {

    public static ClubRecruitmentReadModel from(ClubRecruitment recruitment) {
        return new ClubRecruitmentReadModel(
                recruitment.getId(),
                recruitment.getGeneration(),
                recruitment.isAlwaysOpen(),
                recruitment.getStartsAt(),
                recruitment.getEndsAt(),
                recruitment.isClosedEarly(),
                recruitment.getLinks(),
                recruitment.getNotice());
    }

    public RecruitmentSchedule schedule() {
        return new RecruitmentSchedule(alwaysOpen, startsAt, endsAt, closedEarly);
    }
}
