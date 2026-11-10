package com.donggree.user.internal.application.command;

import java.util.UUID;

/** 인증 코드 원문은 메모리에서만 발송 직전까지 보관한다. */
public record SchoolEmailMailDelivery(
        Long memberId, UUID requestId, String email, String code, String originRequestId, long queuedAtNanos) {
    public SchoolEmailMailDelivery(Long memberId, UUID requestId, String email, String code) {
        this(memberId, requestId, email, code, org.slf4j.MDC.get("request_id"), System.nanoTime());
    }

    @Override
    public String toString() {
        return "SchoolEmailMailDelivery[requestId=" + requestId + "]";
    }
}
