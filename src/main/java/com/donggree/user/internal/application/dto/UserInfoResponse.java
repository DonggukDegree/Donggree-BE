package com.donggree.user.internal.application.dto;

import com.donggree.user.internal.domain.Member;

public record UserInfoResponse(String studentId, String name, String nickname, boolean identityVerified) {
    public static UserInfoResponse from(Member member) {
        return new UserInfoResponse(
                member.getStudentId(), member.getName(), member.getNickname(), member.isIdentityVerified());
    }
}
