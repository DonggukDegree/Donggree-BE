package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.place.internal.application.AdminReviewCommandService;
import com.donggree.place.internal.presentation.swagger.AdminReviewApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자의 리뷰 숨김·숨김 해제 API. 경로는 ADMIN 이상으로 1차, 클래스 단위 @PreAuthorize로 2차 방어한다. */
@RestController
@RequestMapping("/api/admin/reviews/{reviewId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController implements AdminReviewApi {

    private final AdminReviewCommandService adminReviewCommandService;

    @Override
    @PostMapping("/hide")
    public ApiResponse<Void> hideReview(@PathVariable Long reviewId) {
        adminReviewCommandService.hide(reviewId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/unhide")
    public ApiResponse<Void> unhideReview(@PathVariable Long reviewId) {
        adminReviewCommandService.show(reviewId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
