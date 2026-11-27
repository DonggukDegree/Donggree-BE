package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubQueryService;
import com.donggree.club.internal.application.ClubSearch;
import com.donggree.club.internal.application.ClubSort;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import com.donggree.club.internal.presentation.dto.ClubDetailResponse;
import com.donggree.club.internal.presentation.dto.ClubPageResponse;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 동아리 쇼핑 API. 경로 전체가 로그인 필요다. */
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubQueryService clubQueryService;

    /** 필터는 같은 이름을 반복하거나 쉼표로 이어 여러 개를 보낸다. ids는 찜 모아보기처럼 특정 동아리로 좁힐 때만 보낸다. */
    @GetMapping
    public ApiResponse<ClubPageResponse> getClubs(
            @RequestParam(required = false) List<ClubAffiliation> affiliations,
            @RequestParam(required = false) List<ClubField> fields,
            @RequestParam(required = false) List<RecruitmentStatus> recruitmentStatuses,
            @RequestParam(required = false) ClubSort sort,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) String cursor) {
        ClubSearch search = new ClubSearch(
                affiliations == null ? null : new HashSet<>(affiliations),
                fields == null ? null : new HashSet<>(fields),
                recruitmentStatuses == null ? null : new HashSet<>(recruitmentStatuses),
                sort,
                ids == null ? null : new HashSet<>(ids));
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, ClubPageResponse.from(clubQueryService.getClubs(search, cursor)));
    }

    @GetMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> getClub(@PathVariable Long clubId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ClubDetailResponse.from(clubQueryService.getClub(clubId)));
    }
}
