package com.donggree.support.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.support.internal.application.FaqQueryService;
import com.donggree.support.internal.domain.enums.FaqTag;
import com.donggree.support.internal.presentation.dto.FaqResponse;
import com.donggree.support.internal.presentation.swagger.FaqApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자용 FAQ 조회 API(읽기 전용).
 */
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController implements FaqApi {

    private final FaqQueryService faqQueryService;

    @Override
    @GetMapping
    public ApiResponse<List<FaqResponse>> getFaqs(@RequestParam(required = false) FaqTag tag) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                faqQueryService.getFaqs(tag).stream().map(FaqResponse::from).toList());
    }
}
