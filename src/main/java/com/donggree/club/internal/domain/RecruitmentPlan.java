package com.donggree.club.internal.domain;

import java.util.List;

/**
 * 회원·관리자가 입력하는 모집 정보.
 *
 * @param generation 모집 기수. 모르거나 기수가 없으면 null
 * @param notice     모집 안내. 비우면 null
 */
public record RecruitmentPlan(Integer generation, RecruitmentSchedule schedule, List<ClubLink> links, String notice) {

    public RecruitmentPlan {
        if (generation != null && generation < 1) {
            throw new IllegalArgumentException("모집 기수는 1 이상입니다");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("모집 기간을 정해야 합니다");
        }
        links = links == null ? List.of() : List.copyOf(links);
        if (links.size() > ClubLink.MAX_COUNT) {
            throw new IllegalArgumentException("관련 링크는 3개까지입니다");
        }
        notice = notice == null || notice.isBlank() ? null : notice.strip();
        if (notice != null && notice.length() > ClubRecruitment.NOTICE_MAX_LENGTH) {
            throw new IllegalArgumentException("모집 안내는 1000자 이내입니다");
        }
    }
}
