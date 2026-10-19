package com.donggree.user.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.user.internal.application.SchoolEmailVerificationCommandService;
import com.donggree.user.internal.application.UserQueryService;
import com.donggree.user.internal.presentation.dto.SchoolEmailCodeRequest;
import com.donggree.user.internal.presentation.dto.SchoolEmailCodeResponse;
import com.donggree.user.internal.presentation.dto.SchoolEmailCodeVerifyRequest;
import com.donggree.user.internal.presentation.dto.SchoolEmailVerificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/school-email-verification")
@RequiredArgsConstructor
public class SchoolEmailVerificationController {
    private final UserQueryService userQueries;
    private final SchoolEmailVerificationCommandService verifications;

    @GetMapping
    public ApiResponse<SchoolEmailVerificationResponse> getStatus(@LoginMemberId Long memberId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                SchoolEmailVerificationResponse.from(userQueries.getSchoolEmailVerification(memberId)));
    }

    @PostMapping("/codes")
    public ApiResponse<SchoolEmailCodeResponse> requestCode(
            @LoginMemberId Long memberId, @Valid @RequestBody SchoolEmailCodeRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                SchoolEmailCodeResponse.from(verifications.requestCode(memberId, request.toCommand())));
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> verifyCode(
            @LoginMemberId Long memberId, @Valid @RequestBody SchoolEmailCodeVerifyRequest request) {
        verifications.verifyCode(memberId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
