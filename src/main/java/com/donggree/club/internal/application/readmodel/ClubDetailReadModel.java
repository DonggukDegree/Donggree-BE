package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubLink;
import com.donggree.club.internal.domain.ClubOfficer;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 동아리 상세.
 *
 * @param recruitment         현재 모집. 없으면 null(모집 전)
 * @param previousRecruitment 현재 모집이 없을 때 보여줄 가장 최근의 지난 모집. 없으면 null
 * @param canEditDirectly     요청 회원이 승인 없이 바로 수정할 수 있는지
 * @param updatedAt           마지막 정보 등록·수정 시각
 */
public record ClubDetailReadModel(
        Long id,
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        ClubRegistration registration,
        boolean activityConfirmed,
        Integer totalMemberCount,
        LocalDate foundedDate,
        ClubRecruitmentReadModel recruitment,
        String description,
        Integer generation,
        LocalDate activityStartDate,
        LocalDate activityEndDate,
        String roomLocation,
        Integer generationMemberCount,
        List<Image> images,
        Long representativeImageId,
        ClubRecruitmentReadModel previousRecruitment,
        List<ClubOfficer> officers,
        List<ClubLink> links,
        boolean canEditDirectly,
        boolean firstRegistrationRewardAvailable,
        LocalDateTime updatedAt) {

    public record Image(Long id, String url) {}
}
