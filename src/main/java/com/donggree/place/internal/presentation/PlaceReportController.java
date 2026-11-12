package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.place.internal.application.PlaceReportCommandService;
import com.donggree.place.internal.presentation.dto.PlaceReportRequest;
import com.donggree.place.internal.presentation.swagger.PlaceReportApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 동글맵 장소 신고 API. 로그인 필요, 학생 인증 여부는 응용 서비스가 검증한다. */
@RestController
@RequestMapping("/api/places/{placeId}/reports")
@RequiredArgsConstructor
public class PlaceReportController implements PlaceReportApi {

    private final PlaceReportCommandService reportCommandService;

    @Override
    @PostMapping
    public ApiResponse<Long> reportPlace(
            @LoginMemberId Long memberId, @PathVariable Long placeId, @Valid @RequestBody PlaceReportRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, reportCommandService.report(memberId, placeId, request.toCommand()));
    }
}
