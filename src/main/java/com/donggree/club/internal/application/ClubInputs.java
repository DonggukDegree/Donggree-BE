package com.donggree.club.internal.application;

import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.application.command.RecruitmentSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.ClubLink;
import com.donggree.club.internal.domain.ClubOfficer;
import com.donggree.club.internal.domain.ClubProfile;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.RecruitmentPlan;
import com.donggree.club.internal.domain.RecruitmentSchedule;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;

/** 회원·관리자 입력을 도메인 값으로 바꾼다. 입력 규칙을 어기면 400 오류 코드로 바꾼다. */
public final class ClubInputs {

    private ClubInputs() {}

    /** @throws com.donggree.global.apiPayload.exception.GeneralException 입력 규칙 위반(CLUB400_2) */
    public static ClubProfile profile(ClubSaveCommand command) {
        return infoPayload(command).profile();
    }

    static ClubRequestPayload infoPayload(ClubSaveCommand command) {
        try {
            ClubProfile profile = new ClubProfile(
                    command.name(),
                    command.affiliation(),
                    command.fields(),
                    command.description(),
                    command.generation(),
                    command.activityStartDate(),
                    command.activityEndDate(),
                    command.roomLocation(),
                    command.generationMemberCount(),
                    command.totalMemberCount(),
                    command.foundedDate(),
                    command.officers() == null
                            ? List.of()
                            : command.officers().stream()
                                    .map(officer ->
                                            new ClubOfficer(officer.role(), officer.department(), officer.name()))
                                    .toList(),
                    links(command.links()));
            return ClubRequestPayload.of(profile, command.imageCount());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_CLUB);
        }
    }

    static RecruitmentPlan plan(RecruitmentSaveCommand command) {
        try {
            return new RecruitmentPlan(
                    command.generation(),
                    new RecruitmentSchedule(
                            command.alwaysOpen(), command.startsAt(), command.endsAt(), command.closedEarly()),
                    links(command.links()),
                    command.notice());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_RECRUITMENT);
        }
    }

    static ClubRequester requester(ClubRequesterRole role, String contact) {
        try {
            return new ClubRequester(role, contact);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_REQUESTER);
        }
    }

    private static List<ClubLink> links(List<ClubSaveCommand.Link> links) {
        return links == null
                ? List.of()
                : links.stream()
                        .map(link -> new ClubLink(link.label(), link.url()))
                        .toList();
    }
}
