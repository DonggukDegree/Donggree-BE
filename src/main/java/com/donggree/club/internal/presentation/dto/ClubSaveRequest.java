package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDate;
import java.util.List;

/** 동아리 신규 등록·정보 수정·다시 요청 본문. 입력 규칙은 도메인이 검증하고 어기면 400(CLUB400_2)이다. */
public record ClubSaveRequest(
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
        List<Officer> officers,
        List<Link> links,
        List<Long> imageIds,
        Long representativeImageId,
        String requesterContact,
        ClubRequesterRole requesterRole) {

    public record Officer(String role, String department, String name) {}

    public record Link(String label, String url) {}

    public ClubSaveCommand toCommand() {
        return new ClubSaveCommand(
                name,
                affiliation,
                fields,
                description,
                generation,
                activityStartDate,
                activityEndDate,
                roomLocation,
                generationMemberCount,
                totalMemberCount,
                foundedDate,
                officers == null
                        ? null
                        : officers.stream()
                                .map(officer -> new ClubSaveCommand.Officer(
                                        officer.role(), officer.department(), officer.name()))
                                .toList(),
                toLinks(links),
                imageIds,
                representativeImageId,
                requesterContact,
                requesterRole);
    }

    static List<ClubSaveCommand.Link> toLinks(List<Link> links) {
        return links == null
                ? null
                : links.stream()
                        .map(link -> new ClubSaveCommand.Link(link.label(), link.url()))
                        .toList();
    }
}
