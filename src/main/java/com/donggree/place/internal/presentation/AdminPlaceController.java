package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.place.internal.application.AdminPlaceCommandService;
import com.donggree.place.internal.presentation.swagger.AdminPlaceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자의 동글맵 장소 숨김·숨김 해제 API.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 클래스 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin/places/{placeId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlaceController implements AdminPlaceApi {

    private final AdminPlaceCommandService adminPlaceCommandService;

    @Override
    @PostMapping("/hide")
    public ApiResponse<Void> hidePlace(@PathVariable Long placeId) {
        adminPlaceCommandService.hide(placeId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/unhide")
    public ApiResponse<Void> unhidePlace(@PathVariable Long placeId) {
        adminPlaceCommandService.show(placeId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
