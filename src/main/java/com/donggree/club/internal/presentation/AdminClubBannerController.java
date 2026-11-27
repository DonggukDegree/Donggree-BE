package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubBannerQueryService;
import com.donggree.club.internal.application.admin.AdminClubBannerCommandService;
import com.donggree.club.internal.presentation.dto.AdminClubBannerMoveRequest;
import com.donggree.club.internal.presentation.dto.AdminClubBannerResponse;
import com.donggree.club.internal.presentation.dto.AdminClubBannerSaveRequest;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 동아리 쇼핑 배너 관리 API. */
@RestController
@RequestMapping("/api/admin/clubs/banners")
@RequiredArgsConstructor
public class AdminClubBannerController {

    private final ClubBannerQueryService queryService;
    private final AdminClubBannerCommandService commandService;

    @GetMapping
    public ApiResponse<List<AdminClubBannerResponse>> getBanners() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                queryService.getAllBanners().stream()
                        .map(AdminClubBannerResponse::from)
                        .toList());
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody AdminClubBannerSaveRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, commandService.create(request.toCommand()));
    }

    @PutMapping("/{bannerId}")
    public ApiResponse<Void> revise(@PathVariable Long bannerId, @RequestBody AdminClubBannerSaveRequest request) {
        commandService.revise(bannerId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @DeleteMapping("/{bannerId}")
    public ApiResponse<Void> delete(@PathVariable Long bannerId) {
        commandService.delete(bannerId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @PostMapping("/{bannerId}/move")
    public ApiResponse<Void> move(@PathVariable Long bannerId, @RequestBody AdminClubBannerMoveRequest request) {
        commandService.move(bannerId, request.offset());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
