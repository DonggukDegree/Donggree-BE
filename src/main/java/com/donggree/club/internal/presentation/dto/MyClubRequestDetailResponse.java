package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.MyClubRequestDetailReadModel;
import com.donggree.club.internal.domain.ClubProfile;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.RecruitmentPlan;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 요청 한 건과 보냈던 값.
 *
 * @param after 신규 등록·정보 수정은 {@link InfoValue}, 모집 등록은 {@link RecruitmentValue}. 보냈던 연락처·관계를 함께 담는다
 */
public record MyClubRequestDetailResponse(
        Long id,
        ClubRequestType type,
        Long clubId,
        String clubName,
        ClubRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String rejectReason,
        Object after) {

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
            int imageCount,
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

    public static MyClubRequestDetailResponse from(MyClubRequestDetailReadModel detail) {
        var request = detail.request();
        return new MyClubRequestDetailResponse(
                request.id(),
                request.type(),
                request.clubId(),
                request.clubName(),
                request.status(),
                request.requestedAt(),
                request.processedAt(),
                request.rejectReason(),
                value(detail));
    }

    private static Object value(MyClubRequestDetailReadModel detail) {
        ClubRequestPayload after = detail.after();
        if (after.recruitment() != null) {
            RecruitmentPlan plan = after.recruitment();
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
                    detail.requesterContact(),
                    detail.requesterRole());
        }
        ClubProfile profile = after.profile();
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
                after.imageCount(),
                detail.requesterContact(),
                detail.requesterRole());
    }
}
