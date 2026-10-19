package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.command.SchoolEmailCodeRequestResult;
import java.util.UUID;

public record SchoolEmailCodeResponse(
        String email, int expiresInSeconds, int resendAfterSeconds, UUID requestId, String deliveryStatus) {
    public static SchoolEmailCodeResponse from(SchoolEmailCodeRequestResult result) {
        return new SchoolEmailCodeResponse(
                result.email(),
                result.expiresInSeconds(),
                result.resendAfterSeconds(),
                result.requestId(),
                result.deliveryStatus());
    }
}
