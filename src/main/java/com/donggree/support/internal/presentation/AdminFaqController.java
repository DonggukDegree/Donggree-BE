package com.donggree.support.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.support.internal.application.FaqCommandService;
import com.donggree.support.internal.presentation.dto.FaqRequest;
import com.donggree.support.internal.presentation.swagger.AdminFaqApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자의 FAQ 관리 API.
 * 경로(/api/admin/**)는 SecurityConfig에서 ADMIN 이상으로 1차 방어하고, 메서드 단위 @PreAuthorize로 2차 방어한다.
 * 조회는 사용자용 GET /api/faqs 를 그대로 쓴다.
 */
@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFaqController implements AdminFaqApi {

    private final FaqCommandService faqCommandService;

    @Override
    @PostMapping
    public ApiResponse<Long> createFaq(@Valid @RequestBody FaqRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, faqCommandService.create(request.toCommand()));
    }

    @Override
    @PutMapping("/{id}")
    public ApiResponse<Void> updateFaq(@PathVariable Long id, @Valid @RequestBody FaqRequest request) {
        faqCommandService.update(id, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long id) {
        faqCommandService.delete(id);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
