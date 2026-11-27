package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.domain.ClubProfile;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.RecruitmentPlan;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 요청의 변경 전·요청 값. 저장 요청 본문과 같은 모양이며 신규 등록·정보 수정은 {@link InfoValue}, 모집 등록은 {@link RecruitmentValue}다. */
public final class ClubRequestValue {

    private ClubRequestValue() {}

    public record InfoValue(
            String name,
            ClubAffiliation affiliation,
            List<ClubField> fields,
            String description,
            Integer generation,
            LocalDate activityStartDate,
            LocalDate activityEndDate,
            String roomLocation,
            Integer generationMemberCount,
            Integer totalMemberCount,
            LocalDate foundedDate,
            List<ClubDetailResponse.Officer> officers,
            List<ClubRecruitmentResponse.Link> links,
            List<ClubDetailResponse.Image> images,
            Long representativeImageId,
            String requesterContact,
            ClubRequesterRole requesterRole) {}

    public record RecruitmentValue(
            Integer generation,
            boolean alwaysOpen,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            boolean closedEarly,
            List<ClubRecruitmentResponse.Link> links,
            String notice,
            String requesterContact,
            ClubRequesterRole requesterRole) {}

    /** @return 값이 없으면 null */
    /** @param imageUrls 사진 ID별 주소. 정리돼 없어진 사진은 빠진다 */
    public static Object of(
            ClubRequestPayload payload,
            String requesterContact,
            ClubRequesterRole requesterRole,
            Map<Long, String> imageUrls) {
        if (payload == null) return null;
        if (payload.recruitment() != null) {
            RecruitmentPlan plan = payload.recruitment();
            return new RecruitmentValue(
                    plan.generation(),
                    plan.schedule().alwaysOpen(),
                    plan.schedule().startsAt(),
                    plan.schedule().endsAt(),
                    plan.schedule().closedEarly(),
                    plan.links().stream()
                            .map(link -> new ClubRecruitmentResponse.Link(link.label(), link.url()))
                            .toList(),
                    plan.notice(),
                    requesterContact,
                    requesterRole);
        }
        ClubProfile profile = payload.profile();
        return new InfoValue(
                profile.name(),
                profile.affiliation(),
                profile.fields(),
                profile.description(),
                profile.generation(),
                profile.activityStartDate(),
                profile.activityEndDate(),
                profile.roomLocation(),
                profile.generationMemberCount(),
                profile.totalMemberCount(),
                profile.foundedDate(),
                profile.officers().stream()
                        .map(officer ->
                                new ClubDetailResponse.Officer(officer.role(), officer.department(), officer.name()))
                        .toList(),
                profile.links().stream()
                        .map(link -> new ClubRecruitmentResponse.Link(link.label(), link.url()))
                        .toList(),
                payload.photos().imageIds().stream()
                        .filter(imageUrls::containsKey)
                        .map(id -> new ClubDetailResponse.Image(id, imageUrls.get(id)))
                        .toList(),
                payload.photos().representativeImageId(),
                requesterContact,
                requesterRole);
    }
}
