package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.util.List;

/**
 * 목록 카드 한 개.
 *
 * @param imageUrls   대표 사진을 맨 앞에 둔 사진 주소(사진 저장 전까지 빈 목록)
 * @param recruitment 현재 모집. 없으면 null(모집 전)
 */
public record ClubSummaryReadModel(
        Long id,
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        ClubRegistration registration,
        boolean activityConfirmed,
        List<String> imageUrls,
        Integer totalMemberCount,
        LocalDate foundedDate,
        ClubRecruitmentReadModel recruitment) {}
