package com.donggree.curriculum.internal.presentation;

import com.donggree.curriculum.internal.application.RequirementSetCommandService;
import com.donggree.curriculum.internal.application.RequirementSetQueryService;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetRequest;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetResponse;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetSummaryResponse;
import com.donggree.curriculum.internal.presentation.dto.RequirementSetUpdateRequest;
import com.donggree.curriculum.internal.presentation.swagger.AdminRequirementSetApi;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드의 졸업 요건 세트(requirement_set) 관리 API.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 메서드 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin/requirement-sets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequirementSetController implements AdminRequirementSetApi {

    private final RequirementSetQueryService requirementSetQueryService;
    private final RequirementSetCommandService requirementSetCommandService;

    @Override
    @GetMapping
    public ApiResponse<List<RequirementSetSummaryResponse>> getRequirementSets(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                requirementSetQueryService.search(departmentId, collegeId, year).stream()
                        .map(RequirementSetSummaryResponse::from)
                        .toList());
    }

    @Override
    @GetMapping("/{id}")
    public ApiResponse<RequirementSetResponse> getRequirementSet(@PathVariable Long id) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, RequirementSetResponse.from(requirementSetQueryService.get(id)));
    }

    @Override
    @PostMapping
    public ApiResponse<Long> createRequirementSet(@Valid @RequestBody RequirementSetRequest request) {
        Long id = requirementSetCommandService.create(request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, id);
    }

    @Override
    @PutMapping("/{id}")
    public ApiResponse<Void> updateRequirementSet(
            @PathVariable Long id, @Valid @RequestBody RequirementSetUpdateRequest request) {
        requirementSetCommandService.update(id, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
