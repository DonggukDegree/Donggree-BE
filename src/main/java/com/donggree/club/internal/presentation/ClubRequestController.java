package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubCommandService;
import com.donggree.club.internal.application.ClubRequestQueryService;
import com.donggree.club.internal.presentation.dto.ClubSaveRequest;
import com.donggree.club.internal.presentation.dto.ClubSaveResponse;
import com.donggree.club.internal.presentation.dto.MyClubRequestDetailResponse;
import com.donggree.club.internal.presentation.dto.MyClubRequestResponse;
import com.donggree.club.internal.presentation.dto.RecruitmentSaveRequest;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 동아리 요청 내역과 다시 요청. 다른 회원의 요청은 없는 요청과 같게 404다. */
@RestController
@RequestMapping("/api/clubs/me/requests")
@RequiredArgsConstructor
public class ClubRequestController {

    private final ClubRequestQueryService requestQueryService;
    private final ClubCommandService clubCommandService;

    @GetMapping
    public ApiResponse<List<MyClubRequestResponse>> getMyRequests(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                requestQueryService.getMyRequests(memberId).stream()
                        .map(MyClubRequestResponse::from)
                        .toList());
    }

    @GetMapping("/{requestId}")
    public ApiResponse<MyClubRequestDetailResponse> getMyRequest(
            @LoginMemberId Long memberId, @PathVariable Long requestId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                MyClubRequestDetailResponse.from(requestQueryService.getMyRequest(memberId, requestId)));
    }

    /** 신규 등록·정보 수정 요청을 고쳐 다시 보낸다. 검토 중이면 기존 요청은 취소되고 새 요청이 맨 뒤로 들어간다. */
    @PostMapping("/{requestId}/resubmit")
    public ApiResponse<ClubSaveResponse> resubmitInfo(
            @LoginMemberId Long memberId, @PathVariable Long requestId, @RequestBody ClubSaveRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubSaveResponse.from(clubCommandService.resubmitInfo(memberId, requestId, request.toCommand())));
    }

    @PostMapping("/{requestId}/resubmit-recruitment")
    public ApiResponse<ClubSaveResponse> resubmitRecruitment(
            @LoginMemberId Long memberId, @PathVariable Long requestId, @RequestBody RecruitmentSaveRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubSaveResponse.from(
                        clubCommandService.resubmitRecruitment(memberId, requestId, request.toCommand())));
    }
}
