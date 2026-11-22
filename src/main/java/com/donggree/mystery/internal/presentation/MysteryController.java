package com.donggree.mystery.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.mystery.internal.application.*;
import com.donggree.mystery.internal.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 로그인 회원은 학교 인증·성적표 유무와 무관하게 플레이할 수 있다. */
@RestController
@RequestMapping("/api/games/mystery")
@RequiredArgsConstructor
public class MysteryController {
    private final MysteryQueryService queries;
    private final MysteryCommandService commands;

    @GetMapping("/today")
    public ApiResponse<MysteryTodayResponse> today(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, MysteryTodayResponse.from(queries.today(memberId)));
    }

    @PostMapping("/today/guesses")
    public ApiResponse<MysteryTodayResponse> guess(
            @LoginMemberId Long memberId, @Valid @RequestBody MysteryGuessRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, MysteryTodayResponse.from(commands.submit(memberId, request.toCommand())));
    }
}
