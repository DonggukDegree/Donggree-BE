package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.place.internal.application.admin.AdminReportCommandService;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportItem;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportItem;
import com.donggree.place.internal.application.admin.AdminReportQueryService;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.presentation.dto.AdminSuspensionRequest;
import com.donggree.place.internal.presentation.swagger.AdminReportApi;
import com.donggree.user.MemberSuspensionRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 통합 신고 대시보드 API. 관리자 화면 전용 읽기 모델을 그대로 응답한다.
 * 경로(/api/admin/**)는 ADMIN 이상으로 1차, 클래스 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController implements AdminReportApi {

    private final AdminReportQueryService queryService;
    private final AdminReportCommandService commandService;

    @Override
    @GetMapping("/places")
    public ApiResponse<Page<PlaceReportItem>> getPlaceReports(
            @RequestParam(required = false) PlaceReportStatus status, @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getPlaceReports(status, page));
    }

    @Override
    @GetMapping("/places/{reportId}")
    public ApiResponse<PlaceReportDetail> getPlaceReport(@PathVariable Long reportId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getPlaceReport(reportId));
    }

    @Override
    @PostMapping("/places/{reportId}/resolve")
    public ApiResponse<Void> resolvePlaceReport(@PathVariable Long reportId) {
        commandService.resolvePlaceReport(reportId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/places/{reportId}/reject")
    public ApiResponse<Void> rejectPlaceReport(@PathVariable Long reportId) {
        commandService.rejectPlaceReport(reportId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/places/{reportId}/suspensions")
    public ApiResponse<MemberSuspensionRecord> suspendByPlaceReport(
            @LoginMemberId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminSuspensionRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                commandService.suspendByPlaceReport(
                        reportId, request.memberId(), request.days(), request.note(), adminId));
    }

    @Override
    @GetMapping("/reviews")
    public ApiResponse<Page<ReviewReportItem>> getReviewReports(
            @RequestParam(required = false) PlaceReportStatus status, @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getReviewReports(status, page));
    }

    @Override
    @GetMapping("/reviews/{reportId}")
    public ApiResponse<ReviewReportDetail> getReviewReport(@PathVariable Long reportId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, queryService.getReviewReport(reportId));
    }

    @Override
    @PostMapping("/reviews/{reportId}/resolve")
    public ApiResponse<Void> resolveReviewReport(@PathVariable Long reportId) {
        commandService.resolveReviewReport(reportId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/reviews/{reportId}/reject")
    public ApiResponse<Void> rejectReviewReport(@PathVariable Long reportId) {
        commandService.rejectReviewReport(reportId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @PostMapping("/reviews/{reportId}/suspensions")
    public ApiResponse<MemberSuspensionRecord> suspendByReviewReport(
            @LoginMemberId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminSuspensionRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                commandService.suspendByReviewReport(reportId, request.days(), request.note(), adminId));
    }
}
