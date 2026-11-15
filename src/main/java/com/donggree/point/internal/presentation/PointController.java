package com.donggree.point.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.point.internal.application.PointQueryService;
import com.donggree.point.internal.presentation.dto.PointHistoryPageResponse;
import com.donggree.point.internal.presentation.dto.PointSummaryResponse;
import com.donggree.point.internal.presentation.swagger.PointApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 포인트 API. 로그인 필요. */
@RestController
@RequestMapping("/api/points/me")
@RequiredArgsConstructor
public class PointController implements PointApi {

    private final PointQueryService pointQueryService;

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
}
