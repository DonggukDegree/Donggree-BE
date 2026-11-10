package com.donggree.user.internal.application.readmodel;

import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.SchoolEmailDeliveryStatus;
import com.donggree.user.internal.domain.SchoolEmailVerification;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record SchoolEmailVerificationReadModel(
        boolean identityVerified,
        boolean schoolEmailVerified,
        String schoolEmail,
        String deliveryStatus,
        UUID requestId,
        String requestedEmail,
        int expiresInSeconds,
        int resendAfterSeconds) {
    public SchoolEmailVerificationReadModel(boolean identityVerified, boolean schoolEmailVerified, String schoolEmail) {
        this(identityVerified, schoolEmailVerified, schoolEmail, "IDLE", null, null, 0, 0);
    }

    public static SchoolEmailVerificationReadModel from(Member member, SchoolEmailVerification verification) {
        if (verification == null)
            return new SchoolEmailVerificationReadModel(
                    member.isIdentityVerified(), member.isSchoolEmailVerified(), member.getSchoolEmail());
        boolean sent = verification.getDeliveryStatus() == SchoolEmailDeliveryStatus.SENT;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new SchoolEmailVerificationReadModel(
                member.isIdentityVerified(),
                member.isSchoolEmailVerified(),
                member.getSchoolEmail(),
                verification.getDeliveryStatus().name(),
                verification.getRequestId(),
                verification.getEmail(),
                sent ? remaining(now, verification.getExpiresAt()) : 0,
                verification.getDeliveryStatus() != SchoolEmailDeliveryStatus.FAILED
                        ? remaining(now, verification.getSentAt().plusSeconds(SchoolEmailVerification.RESEND_SECONDS))
                        : 0);
    }

    private static int remaining(LocalDateTime now, LocalDateTime deadline) {
        return (int) Math.max(0, (Duration.between(now, deadline).toMillis() + 999) / 1000);
    }
}
