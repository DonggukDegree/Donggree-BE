package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.application.admin.AdminContentQueryService;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceRow;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewRow;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.presentation.swagger.AdminContentApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 관리 화면 하단의 전체 장소·리뷰 목록 API. 숨김·해제는 기존 장소·리뷰 관리자 API를 쓴다.
 * 경로(/api/admin/**)는 ADMIN 이상으로 1차, 클래스 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController implements AdminContentApi {

    private final AdminContentQueryService queryService;

    @Override
    @GetMapping("/places")
    public ApiResponse<Page<PlaceRow>> getPlaces(
            @RequestParam(required = false) PlaceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getPlaces(status, keyword, page));
    }

    @Override
    @GetMapping("/reviews")
    public ApiResponse<Page<ReviewRow>> getReviews(
            @RequestParam(required = false) AdminReviewState state,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getReviews(state, keyword, page));
    }
}
