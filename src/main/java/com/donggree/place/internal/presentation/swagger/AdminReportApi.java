package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportItem;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportItem;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.presentation.dto.AdminSuspensionRequest;
import com.donggree.user.MemberSuspensionRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Report", description = "관리자 통합 신고 대시보드. ADMIN 이상")
public interface AdminReportApi {

    @Operation(summary = "장소 신고 목록", description = "상태별(생략 시 전체) 최신순 20개씩. 신고자 회원 행(이름·학번·단과대·학과) 포함.")
    ApiResponse<Page<PlaceReportItem>> getPlaceReports(
            @Parameter(description = "PENDING·RESOLVED·REJECTED, 생략 시 전체") PlaceReportStatus status,
            @Parameter(description = "0부터 시작하는 페이지") int page);

    @Operation(summary = "장소 신고 상세", description = "같은 장소의 신고 전체, 장소 정보, 등록자, 최근 수정 이력 20건(수정자·전후 값).")
    ApiResponse<PlaceReportDetail> getPlaceReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "장소 신고 처리 완료", description = "대기 신고만 처리할 수 있다(이미 처리하면 409).")
    ApiResponse<Void> resolvePlaceReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "장소 신고 기각", description = "대기 신고만 기각할 수 있다(이미 처리하면 409).")
    ApiResponse<Void> rejectPlaceReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "장소 신고로 회원 정지", description = "등록자·수정자 중 고른 회원을 지금부터 days일 정지하고 신고가 대기면 처리 완료한다.")
    ApiResponse<MemberSuspensionRecord> suspendByPlaceReport(
            Long adminId, @Parameter(description = "신고 ID") Long reportId, AdminSuspensionRequest request);

    @Operation(summary = "리뷰 신고 목록", description = "상태별(생략 시 전체) 최신순 20개씩. 신고자·리뷰 작성자 회원 행 포함.")
    ApiResponse<Page<ReviewReportItem>> getReviewReports(
            @Parameter(description = "PENDING·RESOLVED·REJECTED, 생략 시 전체") PlaceReportStatus status,
            @Parameter(description = "0부터 시작하는 페이지") int page);

    @Operation(summary = "리뷰 신고 상세", description = "같은 리뷰의 신고 전체.")
    ApiResponse<ReviewReportDetail> getReviewReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "리뷰 신고 처리 완료")
    ApiResponse<Void> resolveReviewReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "리뷰 신고 기각")
    ApiResponse<Void> rejectReviewReport(@Parameter(description = "신고 ID") Long reportId);

    @Operation(summary = "리뷰 신고로 작성자 정지", description = "리뷰 작성자를 지금부터 days일 정지하고 신고가 대기면 처리 완료한다. 탈퇴 작성자는 409.")
    ApiResponse<MemberSuspensionRecord> suspendByReviewReport(
            Long adminId, @Parameter(description = "신고 ID") Long reportId, AdminSuspensionRequest request);
}
