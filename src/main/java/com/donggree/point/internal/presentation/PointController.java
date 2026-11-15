package com.donggree.point.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.point.internal.application.PointQueryService;
import com.donggree.point.internal.application.PointRewardService;
import com.donggree.point.internal.presentation.dto.PointHistoryPageResponse;
import com.donggree.point.internal.presentation.dto.PointRewardAcknowledgeRequest;
import com.donggree.point.internal.presentation.dto.PointRewardAcknowledgeResponse;
import com.donggree.point.internal.presentation.dto.PointSummaryResponse;
import com.donggree.point.internal.presentation.swagger.PointApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 포인트 API. 로그인 필요. */
@RestController
@RequestMapping("/api/points/me")
@RequiredArgsConstructor
public class PointController implements PointApi {

    private final PointQueryService pointQueryService;
    private final PointRewardService pointRewardService;

    @Override
    @GetMapping
    public ApiResponse<PointSummaryResponse> getSummary(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, new PointSummaryResponse(pointQueryService.getTotalPoints(memberId)));
    }

    @Override
    @GetMapping("/histories")
    public ApiResponse<PointHistoryPageResponse> getHistories(
            @LoginMemberId Long memberId, @RequestParam(required = false) Long cursor) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, PointHistoryPageResponse.from(pointQueryService.getHistories(memberId, cursor)));
    }

    @Override
    @PostMapping("/rewards/acknowledge")
    public ApiResponse<PointRewardAcknowledgeResponse> acknowledgeReward(
            @LoginMemberId Long memberId, @Valid @RequestBody PointRewardAcknowledgeRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                pointRewardService
                        .acknowledge(memberId, request.reason(), request.targetId())
                        .map(amount -> new PointRewardAcknowledgeResponse(true, amount))
                        .orElseGet(() -> new PointRewardAcknowledgeResponse(false, 0)));
    }
}
