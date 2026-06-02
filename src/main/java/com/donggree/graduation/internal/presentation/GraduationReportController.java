package com.donggree.graduation.internal.presentation;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.graduation.internal.application.GraduationReportService;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.donggree.graduation.internal.presentation.swagger.GraduationReportApi;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class GraduationReportController implements GraduationReportApi {

    private final GraduationReportService graduationReportService;

    @Override
    @GetMapping("/{reportId}/summary")
    public ApiResponse<GraduationReportResponse> getReport(
            @Parameter(description = "리포트 ID (= 성적표 ID)") @PathVariable Long reportId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, graduationReportService.getReport(reportId));
    }

    @Override
    @GetMapping("/{reportId}")
    public ApiResponse<AreaDetailResponse> getAreaDetail(
            @Parameter(description = "리포트 ID (= 성적표 ID)") @PathVariable Long reportId,
            @Parameter(description = "이수 구분 코드 (COMMON_GENERAL, ACADEMIC_FOUNDATION, FIRST_MAJOR 등)") @RequestParam
                    CourseType courseType) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, graduationReportService.getAreaDetail(reportId, courseType));
    }
}
