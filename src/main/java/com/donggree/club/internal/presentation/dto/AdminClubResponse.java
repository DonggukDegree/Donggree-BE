package com.donggree.club.internal.presentation.dto;

import com.donggree.club.internal.application.admin.AdminClubReadModel;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 동아리 목록 한 개. 목록 카드 정보에 직접 수정 권한 회원과 마지막 정보 수정 시각을 더한다. */
public record AdminClubResponse(
        Long id,
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        ClubRegistration registration,
        boolean activityConfirmed,
        List<String> imageUrls,
        Integer totalMemberCount,
        LocalDate foundedDate,
        ClubRecruitmentResponse recruitment,
        List<Editor> editors,
        LocalDateTime updatedAt) {

    public record Editor(Long memberId, String name, LocalDateTime grantedAt) {}

    public static AdminClubResponse from(AdminClubReadModel model) {
        var club = model.club();
        return new AdminClubResponse(
                club.id(),
                club.name(),
                club.affiliation(),
                club.fields(),
                club.registration(),
                club.activityConfirmed(),
                club.imageUrls(),
                club.totalMemberCount(),
                club.foundedDate(),
                ClubRecruitmentResponse.from(club.recruitment()),
                model.editors().stream()
                        .map(editor -> new Editor(editor.memberId(), editor.name(), editor.grantedAt()))
                        .toList(),
                model.updatedAt());
    }
}
