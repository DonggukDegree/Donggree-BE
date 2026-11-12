package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.place.internal.application.PlaceReviewCommandService;
import com.donggree.place.internal.application.PlaceReviewQueryService;
import com.donggree.place.internal.application.ReviewReportCommandService;
import com.donggree.place.internal.presentation.dto.ReviewPageResponse;
import com.donggree.place.internal.presentation.dto.ReviewReportRequest;
import com.donggree.place.internal.presentation.dto.ReviewRequest;
import com.donggree.place.internal.presentation.swagger.PlaceReviewApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 동글맵 장소 리뷰 API. 로그인 필요, 학생 인증·작성자 확인은 응용 서비스가 한다. */
@RestController
@RequestMapping("/api/places/{placeId}/reviews")
@RequiredArgsConstructor
public class PlaceReviewController implements PlaceReviewApi {

    private final PlaceReviewQueryService reviewQueryService;
    private final PlaceReviewCommandService reviewCommandService;
    private final ReviewReportCommandService reviewReportCommandService;

    @Override
    @GetMapping
    public ApiResponse<ReviewPageResponse> getReviews(
            @LoginMemberId Long memberId, @PathVariable Long placeId, @RequestParam(required = false) Long cursor) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ReviewPageResponse.from(reviewQueryService.getReviews(memberId, placeId, cursor)));
    }

    @Override
    @PostMapping
    public ApiResponse<Long> writeReview(
            @LoginMemberId Long memberId, @PathVariable Long placeId, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, reviewCommandService.write(memberId, placeId, request.toCommand()));
    }

    @Override
    @PutMapping("/{reviewId}")
    public ApiResponse<Void> editReview(
            @LoginMemberId Long memberId,
            @PathVariable Long placeId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {
        reviewCommandService.edit(memberId, placeId, reviewId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @LoginMemberId Long memberId, @PathVariable Long placeId, @PathVariable Long reviewId) {
        reviewCommandService.delete(memberId, placeId, reviewId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/{reviewId}/reports")
    public ApiResponse<Long> reportReview(
            @LoginMemberId Long memberId,
            @PathVariable Long placeId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReportRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                reviewReportCommandService.report(memberId, placeId, reviewId, request.toCommand()));
    }
}
