package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubDetailReadModel;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClubDetailResponse(
        Long id,
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        ClubRegistration registration,
        boolean activityConfirmed,
        Integer totalMemberCount,
        LocalDate foundedDate,
        ClubRecruitmentResponse recruitment,
        String description,
        Integer generation,
        LocalDate activityStartDate,
        LocalDate activityEndDate,
        String roomLocation,
        Integer generationMemberCount,
        List<Image> images,
        Long representativeImageId,
        ClubRecruitmentResponse previousRecruitment,
        List<Officer> officers,
        List<ClubRecruitmentResponse.Link> links,
        boolean canEditDirectly,
        boolean firstRegistrationRewardAvailable,
        LocalDateTime updatedAt) {

    public record Image(Long id, String url) {}

    public record Officer(String role, String department, String name) {}

    public static ClubDetailResponse from(ClubDetailReadModel club) {
        return new ClubDetailResponse(
                club.id(),
                club.name(),
                club.affiliation(),
                club.fields(),
                club.registration(),
                club.activityConfirmed(),
                club.totalMemberCount(),
                club.foundedDate(),
                ClubRecruitmentResponse.from(club.recruitment()),
                club.description(),
                club.generation(),
                club.activityStartDate(),
                club.activityEndDate(),
                club.roomLocation(),
                club.generationMemberCount(),
                club.images().stream()
                        .map(image -> new Image(image.id(), image.url()))
                        .toList(),
                club.representativeImageId(),
                ClubRecruitmentResponse.from(club.previousRecruitment()),
                club.officers().stream()
                        .map(officer -> new Officer(officer.role(), officer.department(), officer.name()))
                        .toList(),
                club.links().stream()
                        .map(link -> new ClubRecruitmentResponse.Link(link.label(), link.url()))
                        .toList(),
                club.canEditDirectly(),
                club.firstRegistrationRewardAvailable(),
                club.updatedAt());
    }
}
