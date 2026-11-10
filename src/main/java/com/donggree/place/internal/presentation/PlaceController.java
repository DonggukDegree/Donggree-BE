package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.place.internal.application.PlaceCommandService;
import com.donggree.place.internal.application.PlaceQueryService;
import com.donggree.place.internal.presentation.dto.PlaceDetailResponse;
import com.donggree.place.internal.presentation.dto.PlaceDuplicatesResponse;
import com.donggree.place.internal.presentation.dto.PlaceRegisterRequest;
import com.donggree.place.internal.presentation.dto.PlaceSummaryResponse;
import com.donggree.place.internal.presentation.swagger.PlaceApi;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 동글맵 장소 API. 경로 전체가 로그인 필요이며, 쓰기 권한은 응용 서비스가 검증한다. */
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController implements PlaceApi {

    private final PlaceQueryService placeQueryService;
    private final PlaceCommandService placeCommandService;

    @Override
    @GetMapping
    public ApiResponse<List<PlaceSummaryResponse>> getPlaces() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                placeQueryService.getPlaces().stream()
                        .map(PlaceSummaryResponse::from)
                        .toList());
    }

    @Override
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailResponse> getPlace(@PathVariable Long placeId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, PlaceDetailResponse.from(placeQueryService.getPlace(placeId)));
    }

    @Override
    @GetMapping("/duplicates")
    public ApiResponse<PlaceDuplicatesResponse> findDuplicates(
            @RequestParam String name, @RequestParam double latitude, @RequestParam double longitude) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                PlaceDuplicatesResponse.from(placeQueryService.findDuplicates(name, latitude, longitude)));
    }

    @Override
    @PostMapping
    public ApiResponse<Long> registerPlace(
            @LoginMemberId Long memberId, @Valid @RequestBody PlaceRegisterRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, placeCommandService.register(memberId, request.toCommand()));
    }
}
