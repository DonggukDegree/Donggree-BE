package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.presentation.dto.ReviewPageResponse;
import com.donggree.place.internal.presentation.dto.ReviewReportRequest;
import com.donggree.place.internal.presentation.dto.ReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Place Review", description = "동글맵 장소 리뷰. 조회는 로그인 회원, 작성·수정·신고는 인증 학생, 수정·삭제는 본인")
public interface PlaceReviewApi {

    @Operation(
            summary = "리뷰 목록",
            description = "보이는 리뷰를 최신순으로 10개씩 반환한다. 다음 페이지는 nextCursor를 cursor로 넘긴다. "
                    + "요청 회원의 리뷰는 숨겨졌어도 myReview로 함께 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 숨김 장소")
    })
    ApiResponse<ReviewPageResponse> getReviews(
            Long memberId,
            @Parameter(description = "장소 ID") Long placeId,
            @Parameter(description = "이전 페이지의 nextCursor. 첫 페이지는 생략") Long cursor);

    @Operation(summary = "리뷰 작성", description = "인증 학생이 장소마다 1건 작성한다(삭제 후 다시 작성 가능). 생성된 리뷰 ID를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "별점·본문 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "학생 인증 미완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 숨김 장소"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 리뷰를 작성함")
    })
    ApiResponse<Long> writeReview(Long memberId, @Parameter(description = "장소 ID") Long placeId, ReviewRequest request);

    @Operation(summary = "리뷰 수정", description = "본인 리뷰의 별점·본문을 고친다. 숨겨진 리뷰는 수정할 수 없다(409).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "학생 인증 미완료 또는 본인 리뷰 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 장소·리뷰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "숨겨진 리뷰")
    })
    ApiResponse<Void> editReview(
            Long memberId,
            @Parameter(description = "장소 ID") Long placeId,
            @Parameter(description = "리뷰 ID") Long reviewId,
            ReviewRequest request);

    @Operation(summary = "리뷰 삭제", description = "본인 리뷰를 삭제한다. 신고 이력 보존을 위해 행은 남기고 목록·평균·1건 제한에서 뺀다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 리뷰 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 장소·리뷰")
    })
    ApiResponse<Void> deleteReview(
            Long memberId,
            @Parameter(description = "장소 ID") Long placeId,
            @Parameter(description = "리뷰 ID") Long reviewId);

    @Operation(summary = "리뷰 신고", description = "인증 학생이 다른 회원의 리뷰를 신고한다. 서로 다른 회원 3명의 처리 대기 신고가 쌓이면 리뷰를 자동 숨김한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 접수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류, 본인 리뷰 신고"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "학생 인증 미완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 숨긴 장소·리뷰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "처리 대기 신고 존재")
    })
    ApiResponse<Long> reportReview(
            Long memberId,
            @Parameter(description = "장소 ID") Long placeId,
            @Parameter(description = "리뷰 ID") Long reviewId,
            ReviewReportRequest request);
}
