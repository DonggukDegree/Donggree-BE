package com.donggree.graduation.internal.presentation;

import com.donggree.curriculum.CourseType;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.graduation.internal.application.GraduationQueryService;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
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

    private final GraduationQueryService graduationQueryService;

    @Override
    @GetMapping("/summary")
    public ApiResponse<GraduationReportProjection> getReport(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, graduationQueryService.getReport(memberId));
    }

    @Override
    @GetMapping
    public ApiResponse<AreaDetailProjection> getAreaDetail(
            @LoginMemberId Long memberId, @RequestParam CourseType courseType) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, graduationQueryService.getAreaDetail(memberId, courseType));
    }
}
