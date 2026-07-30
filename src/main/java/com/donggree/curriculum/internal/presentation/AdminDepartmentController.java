package com.donggree.curriculum.internal.presentation;

import com.donggree.curriculum.internal.application.DepartmentQueryService;
import com.donggree.curriculum.internal.presentation.dto.CollegeResponse;
import com.donggree.curriculum.internal.presentation.dto.DepartmentResponse;
import com.donggree.curriculum.internal.presentation.swagger.AdminDepartmentApi;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드의 단과대·학과 조회 API. 졸업 요건 세트 필터·등록 드롭다운의 선택지를 제공한다.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 메서드 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController implements AdminDepartmentApi {

    private final DepartmentQueryService departmentQueryService;

    @Override
    @GetMapping("/colleges")
    public ApiResponse<List<CollegeResponse>> getColleges() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                departmentQueryService.getColleges().stream()
                        .map(CollegeResponse::from)
                        .toList());
    }

    @Override
    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponse>> getDepartments(@RequestParam(required = false) Long collegeId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                departmentQueryService.getDepartments(collegeId).stream()
                        .map(DepartmentResponse::from)
                        .toList());
    }
}
