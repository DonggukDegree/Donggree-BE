package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.readmodel.UserInfoReadModel;

/**
 * GET/PATCH /api/users/me 응답 DTO. 학번·이름·닉네임·PDF 본인인증·학교 이메일 인증 여부를 담는다.
 */
public record UserInfoResponse(
        String studentId, String name, String nickname, boolean identityVerified, boolean schoolEmailVerified) {
    public static UserInfoResponse from(UserInfoReadModel projection) {
        return new UserInfoResponse(
                projection.studentId(),
                projection.name(),
                projection.nickname(),
                projection.identityVerified(),
                projection.schoolEmailVerified());
    }
}
