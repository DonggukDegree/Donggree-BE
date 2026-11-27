package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubBannerQueryService;
import com.donggree.club.internal.application.ClubCommandService;
import com.donggree.club.internal.application.ClubQueryService;
import com.donggree.club.internal.application.ClubSearch;
import com.donggree.club.internal.application.ClubSort;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import com.donggree.club.internal.presentation.dto.ClubBannerResponse;
import com.donggree.club.internal.presentation.dto.ClubDetailResponse;
import com.donggree.club.internal.presentation.dto.ClubPageResponse;
import com.donggree.club.internal.presentation.dto.ClubSaveRequest;
import com.donggree.club.internal.presentation.dto.ClubSaveResponse;
import com.donggree.club.internal.presentation.dto.RecruitmentSaveRequest;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 동아리 쇼핑 API. 경로 전체가 로그인 필요이며, 쓰기의 학생 인증·직접 수정 권한은 응용 서비스가 검증한다. */
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubQueryService clubQueryService;
    private final ClubCommandService clubCommandService;
    private final ClubBannerQueryService bannerQueryService;

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

    /** 켜져 있고 노출 기간 안인 배너만 노출 순서대로. */
    @GetMapping("/banners")
    public ApiResponse<List<ClubBannerResponse>> getBanners() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                bannerQueryService.getVisibleBanners().stream()
                        .map(ClubBannerResponse::from)
                        .toList());
    }

    @GetMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> getClub(@LoginMemberId Long memberId, @PathVariable Long clubId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, ClubDetailResponse.from(clubQueryService.getClub(memberId, clubId)));
    }

    /** 신규 등록은 항상 관리자 승인 요청이다. */
    @PostMapping
    public ApiResponse<ClubSaveResponse> register(@LoginMemberId Long memberId, @RequestBody ClubSaveRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubSaveResponse.from(clubCommandService.register(memberId, request.toCommand())));
    }

    @PutMapping("/{clubId}")
    public ApiResponse<ClubSaveResponse> edit(
            @LoginMemberId Long memberId, @PathVariable Long clubId, @RequestBody ClubSaveRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubSaveResponse.from(clubCommandService.edit(memberId, clubId, request.toCommand())));
    }

    @PutMapping("/{clubId}/recruitment")
    public ApiResponse<ClubSaveResponse> saveRecruitment(
            @LoginMemberId Long memberId, @PathVariable Long clubId, @RequestBody RecruitmentSaveRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubSaveResponse.from(clubCommandService.saveRecruitment(memberId, clubId, request.toCommand())));
    }
}
