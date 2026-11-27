package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import java.time.LocalDateTime;
import java.util.List;

/** 모집 건 한 개. 목록·상세가 같이 쓴다. 일시는 한국 시간이다. */
public record ClubRecruitmentResponse(
        Long id,
        Integer generation,
        boolean alwaysOpen,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean closedEarly,
        List<Link> links,
        String notice) {

    public record Link(String label, String url) {}

    /** @return 모집 건이 없으면 null */
    public static ClubRecruitmentResponse from(ClubRecruitmentReadModel recruitment) {
        if (recruitment == null) return null;
        return new ClubRecruitmentResponse(
                recruitment.id(),
                recruitment.generation(),
                recruitment.alwaysOpen(),
                recruitment.startsAt(),
                recruitment.endsAt(),
                recruitment.closedEarly(),
                recruitment.links().stream()
                        .map(link -> new Link(link.label(), link.url()))
                        .toList(),
                recruitment.notice());
    }
}
