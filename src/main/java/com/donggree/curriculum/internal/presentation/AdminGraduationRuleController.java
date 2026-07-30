package com.donggree.curriculum.internal.presentation;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.GraduationRuleCommandService;
import com.donggree.curriculum.internal.application.GraduationRuleQueryService;
import com.donggree.curriculum.internal.presentation.dto.GraduationRuleBatchRequest;
import com.donggree.curriculum.internal.presentation.dto.GraduationRuleResponse;
import com.donggree.curriculum.internal.presentation.dto.RuleTypeResponse;
import com.donggree.curriculum.internal.presentation.swagger.AdminGraduationRuleApi;
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
 * 관리자 대시보드의 졸업 규칙(graduation_rule)·규칙 종류(rule_type) 관리 API.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 메서드 단위 @PreAuthorize로 2차 방어한다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGraduationRuleController implements AdminGraduationRuleApi {

    private final GraduationRuleQueryService graduationRuleQueryService;
    private final GraduationRuleCommandService graduationRuleCommandService;

    @Override
    @GetMapping("/rule-types")
    public ApiResponse<List<RuleTypeResponse>> getRuleTypes() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                graduationRuleQueryService.getRuleTypes().stream()
                        .map(RuleTypeResponse::from)
                        .toList());
    }

    @Override
    @GetMapping("/graduation-rules")
    public ApiResponse<List<GraduationRuleResponse>> getGraduationRules(
            @RequestParam(required = false) List<Long> ruleTypeIds,
            @RequestParam(required = false) List<CourseType> courseTypes) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                graduationRuleQueryService.search(ruleTypeIds, courseTypes).stream()
                        .map(GraduationRuleResponse::from)
                        .toList());
    }

    @Override
    @PutMapping("/graduation-rules")
    public ApiResponse<List<Long>> upsertGraduationRules(@Valid @RequestBody GraduationRuleBatchRequest request) {
        List<Long> ids = graduationRuleCommandService.upsert(request.toCommands());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ids);
    }
}
