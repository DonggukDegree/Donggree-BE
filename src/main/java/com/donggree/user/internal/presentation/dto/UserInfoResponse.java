package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.projection.UserInfoProjection;

/**
 * GET/PATCH /api/users/me 응답 DTO. 학번·이름·닉네임·본인인증여부를 담는다.
 */
public record UserInfoResponse(String studentId, String name, String nickname, boolean identityVerified) {
    public static UserInfoResponse from(UserInfoProjection projection) {
        return new UserInfoResponse(
                projection.studentId(), projection.name(), projection.nickname(), projection.identityVerified());
    }
}
