package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubActivity;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 목록 계산에 필요한 동아리 한 개의 저장 값. 활동 확인·현재 모집·필터·정렬은 응용 계층이 조회 시각 기준으로 계산한다.
 *
 * @param imageUrls         대표 사진을 맨 앞에 둔 사진 주소
 * @param latestRecruitment 가장 최근 모집 건. 없으면 null
 */
public record ClubListRow(
        Long id,
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        ClubRegistration registration,
        Integer generation,
        LocalDate activityStartDate,
        LocalDate activityEndDate,
        LocalDateTime infoUpdatedAt,
        Integer totalMemberCount,
        LocalDate foundedDate,
        List<String> imageUrls,
        ClubRecruitmentReadModel latestRecruitment) {

    public ClubActivity activity() {
        return new ClubActivity(generation, activityStartDate, activityEndDate, infoUpdatedAt);
    }
}
