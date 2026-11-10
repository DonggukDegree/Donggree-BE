package com.donggree.user.internal.application.command;

import java.util.UUID;

public record SchoolEmailCodeRequestResult(
        String email, int expiresInSeconds, int resendAfterSeconds, UUID requestId, String deliveryStatus) {
    public SchoolEmailCodeRequestResult(String email, int expiresInSeconds, int resendAfterSeconds) {
        this(email, expiresInSeconds, resendAfterSeconds, null, "QUEUED");
    }
}
