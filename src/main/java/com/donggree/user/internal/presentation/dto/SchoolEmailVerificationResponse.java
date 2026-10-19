package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.readmodel.SchoolEmailVerificationReadModel;
import java.util.UUID;

public record SchoolEmailVerificationResponse(
        boolean identityVerified,
        boolean schoolEmailVerified,
        String schoolEmail,
        String deliveryStatus,
        UUID requestId,
        String requestedEmail,
        int expiresInSeconds,
        int resendAfterSeconds) {
    public static SchoolEmailVerificationResponse from(SchoolEmailVerificationReadModel result) {
        return new SchoolEmailVerificationResponse(
                result.identityVerified(),
                result.schoolEmailVerified(),
                result.schoolEmail(),
                result.deliveryStatus(),
                result.requestId(),
                result.requestedEmail(),
                result.expiresInSeconds(),
                result.resendAfterSeconds());
    }
}
