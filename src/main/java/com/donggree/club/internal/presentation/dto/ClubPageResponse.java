package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.readmodel.ClubPageReadModel;
import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.util.List;

public record ClubPageResponse(List<Summary> clubs, String nextCursor, int totalCount) {

    public record Summary(
            Long id,
            String name,
            ClubAffiliation affiliation,
            List<ClubField> fields,
            ClubRegistration registration,
            boolean activityConfirmed,
            List<String> imageUrls,
            Integer totalMemberCount,
            LocalDate foundedDate,
            ClubRecruitmentResponse recruitment) {

        static Summary from(ClubSummaryReadModel club) {
            return new Summary(
                    club.id(),
                    club.name(),
                    club.affiliation(),
                    club.fields(),
                    club.registration(),
                    club.activityConfirmed(),
                    club.imageUrls(),
                    club.totalMemberCount(),
                    club.foundedDate(),
                    ClubRecruitmentResponse.from(club.recruitment()));
        }
    }

    public static ClubPageResponse from(ClubPageReadModel page) {
        return new ClubPageResponse(
                page.clubs().stream().map(Summary::from).toList(), page.nextCursor(), page.totalCount());
    }
}
