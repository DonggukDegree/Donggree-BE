package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceRow;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewRow;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Content", description = "신고 관리 하단 전체 장소·리뷰 목록. ADMIN 이상")
public interface AdminContentApi {

    @Operation(summary = "장소 목록", description = "등록 최신순 20개씩. 상태·처리 대기 신고 수·등록자 포함.")
    ApiResponse<Page<PlaceRow>> getPlaces(
            @Parameter(description = "VISIBLE·HIDDEN(생략 시 전체)") PlaceStatus status,
            @Parameter(description = "장소 이름 검색어(생략 시 전체)") String keyword,
            @Parameter(description = "0부터 시작하는 페이지") int page);

    @Operation(summary = "리뷰 목록", description = "작성 최신순 20개씩. 숨긴·지운 리뷰도 본문까지 포함. 상태·처리 대기 신고 수·작성자 포함.")
    ApiResponse<Page<ReviewRow>> getReviews(
            @Parameter(description = "VISIBLE·HIDDEN·DELETED(생략 시 전체)") AdminReviewState state,
            @Parameter(description = "리뷰 본문 또는 장소 이름 검색어(생략 시 전체)") String keyword,
            @Parameter(description = "0부터 시작하는 페이지") int page);
}
