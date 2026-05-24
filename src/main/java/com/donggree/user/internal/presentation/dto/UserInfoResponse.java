package com.donggree.user.internal.presentation.dto;

public record UserInfoResponse(
        String studentId,
        String name,
        String nickname
) {
}
