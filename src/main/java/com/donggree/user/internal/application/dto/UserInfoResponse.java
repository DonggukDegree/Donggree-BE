package com.donggree.user.internal.application.dto;

public record UserInfoResponse(
        String studentId,
        String name,
        String nickname
) {
}
