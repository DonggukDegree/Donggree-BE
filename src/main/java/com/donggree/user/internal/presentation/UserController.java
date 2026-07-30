package com.donggree.user.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.user.internal.application.UserCommandService;
import com.donggree.user.internal.application.UserQueryService;
import com.donggree.user.internal.presentation.dto.OnboardingRequest;
import com.donggree.user.internal.presentation.dto.UserInfoResponse;
import com.donggree.user.internal.presentation.dto.UserInfoUpdateRequest;
import com.donggree.user.internal.presentation.swagger.UserApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Override
    @PostMapping("/onboarding")
    public ApiResponse<Void> completeOnboarding(
            @LoginMemberId Long memberId, @Valid @RequestBody OnboardingRequest request) {
        userCommandService.completeOnboarding(memberId, request.studentId(), request.name());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @Override
    @GetMapping
    public ApiResponse<UserInfoResponse> getUserInfo(@LoginMemberId Long memberId) {
        UserInfoResponse response = UserInfoResponse.from(userQueryService.getUserInfo(memberId));
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    @Override
    @PatchMapping
    public ApiResponse<UserInfoResponse> updateUserInfo(
            @LoginMemberId Long memberId, @Valid @RequestBody UserInfoUpdateRequest request) {
        UserInfoResponse response = UserInfoResponse.from(
                userCommandService.updateUserInfo(memberId, request.studentId(), request.name(), request.nickname()));
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    @Override
    @DeleteMapping
    public ApiResponse<Void> deleteUser(@LoginMemberId Long memberId) {
        userCommandService.deleteUser(memberId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
