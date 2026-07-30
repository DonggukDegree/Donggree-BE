package com.donggree.curriculum.internal.presentation;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.CourseClassificationCommandService;
import com.donggree.curriculum.internal.application.CourseClassificationQueryService;
import com.donggree.curriculum.internal.presentation.dto.AreaTypeResponse;
import com.donggree.curriculum.internal.presentation.dto.CourseClassificationBatchRequest;
import com.donggree.curriculum.internal.presentation.dto.CourseClassificationResponse;
import com.donggree.curriculum.internal.presentation.swagger.AdminCurriculumApi;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드의 커리큘럼(과목 분류·이수 영역) 관리 API.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 메서드 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCurriculumController implements AdminCurriculumApi {

    private final CourseClassificationQueryService courseClassificationQueryService;
    private final CourseClassificationCommandService courseClassificationCommandService;

    @Override
    @GetMapping("/course-classifications")
    public ApiResponse<List<CourseClassificationResponse>> getCourseClassifications(
            @RequestParam(required = false) List<Long> areaTypeIds,
            @RequestParam(required = false) List<CourseType> courseTypes,
            @RequestParam(required = false) List<Integer> years) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                courseClassificationQueryService.search(areaTypeIds, courseTypes, years).stream()
                        .map(CourseClassificationResponse::from)
                        .toList());
    }

    @Override
    @PutMapping("/course-classifications")
    public ApiResponse<List<Long>> upsertCourseClassifications(
            @Valid @RequestBody CourseClassificationBatchRequest request) {
        List<Long> ids = courseClassificationCommandService.upsert(request.toCommands());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ids);
    }

    @Override
    @GetMapping("/area-types")
    public ApiResponse<List<AreaTypeResponse>> getAreaTypes() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                courseClassificationQueryService.getAreaTypes().stream()
                        .map(AreaTypeResponse::from)
                        .toList());
    }
}
