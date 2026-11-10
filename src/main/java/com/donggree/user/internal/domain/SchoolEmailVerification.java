package com.donggree.user.internal.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

/** 회원별 최신 인증 요청. 다른 주소로 재요청해도 발송 제한 횟수는 유지한다. */
@Getter
public class SchoolEmailVerification {
    public static final int EXPIRY_SECONDS = 300;
    public static final int RESEND_SECONDS = 60;
    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_DAILY_SENDS = 10;

    private final Long memberId;

    private String email;

    private String codeHash;

    private LocalDateTime expiresAt;

    private LocalDateTime sentAt;

    private int attempts;

    private LocalDate sendDate;

    private int dailySends;

    private SchoolEmailDeliveryStatus deliveryStatus = SchoolEmailDeliveryStatus.SENT;

    private UUID requestId;

    public SchoolEmailVerification(Long memberId) {
        this.memberId = memberId;
    }

    public SchoolEmailVerification copy() {
        var copy = new SchoolEmailVerification(memberId);
        copy.email = email;
        copy.codeHash = codeHash;
        copy.expiresAt = expiresAt;
        copy.sentAt = sentAt;
        copy.attempts = attempts;
        copy.sendDate = sendDate;
        copy.dailySends = dailySends;
        copy.deliveryStatus = deliveryStatus;
        copy.requestId = requestId;
        return copy;
    }

    public boolean canResend(LocalDateTime now) {
        if (deliveryStatus == SchoolEmailDeliveryStatus.FAILED) return true;
        return sentAt == null || !now.isBefore(sentAt.plusSeconds(RESEND_SECONDS));
    }

    public boolean reachedDailyLimit(LocalDateTime now) {
        return now.toLocalDate().equals(sendDate) && dailySends >= MAX_DAILY_SENDS;
    }

    public void issue(SchoolEmailAddress address, String hash, LocalDateTime now) {
        if (!canResend(now) || reachedDailyLimit(now)) {
            throw new IllegalStateException("메일 발송 제한을 초과했습니다.");
        }
        dailySends = now.toLocalDate().equals(sendDate) ? dailySends + 1 : 1;
        sendDate = now.toLocalDate();
        email = address.value();
        codeHash = hash;
        sentAt = now;
        expiresAt = now.plusSeconds(EXPIRY_SECONDS);
        attempts = 0;
    }

    public void enqueue(SchoolEmailAddress address, String hash, UUID requestId, LocalDateTime now) {
        issue(address, hash, now);
        this.requestId = requestId;
        deliveryStatus = SchoolEmailDeliveryStatus.QUEUED;
    }

    public boolean isQueued(UUID expectedRequestId) {
        return deliveryStatus == SchoolEmailDeliveryStatus.QUEUED && expectedRequestId.equals(requestId);
    }

    public void startDelivery() {
        if (deliveryStatus != SchoolEmailDeliveryStatus.QUEUED)
            throw new IllegalStateException("대기 중인 메일만 발송할 수 있습니다.");
        deliveryStatus = SchoolEmailDeliveryStatus.SENDING;
    }

    public boolean isSending(UUID expectedRequestId) {
        return deliveryStatus == SchoolEmailDeliveryStatus.SENDING && expectedRequestId.equals(requestId);
    }

    public void finishDelivery(LocalDateTime now) {
        deliveryStatus = SchoolEmailDeliveryStatus.SENT;
        // 재발송 제한은 요청 접수 시각, 코드 유효 시간은 실제 발송 완료 시각 기준이다.
        expiresAt = now.plusSeconds(EXPIRY_SECONDS);
    }

    public void failDelivery() {
        deliveryStatus = SchoolEmailDeliveryStatus.FAILED;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLocked() {
        return attempts >= MAX_ATTEMPTS;
    }

    public void recordFailure() {
        attempts = Math.min(MAX_ATTEMPTS, attempts + 1);
    }
}
