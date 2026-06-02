package com.donggree.graduation.internal.presentation;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.graduation.internal.application.GraduationReportService;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.donggree.graduation.internal.presentation.swagger.GraduationReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class GraduationReportController implements GraduationReportApi {

    private final GraduationReportService graduationReportService;

    @Override
    @GetMapping("/summary")
    public ApiResponse<GraduationReportResponse> getReport(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, graduationReportService.getReport(memberId));
    }

    @Override
    @GetMapping
    public ApiResponse<AreaDetailResponse> getAreaDetail(
            @LoginMemberId Long memberId, @RequestParam CourseType courseType) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, graduationReportService.getAreaDetail(memberId, courseType));
    }
}
