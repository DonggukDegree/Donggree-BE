package com.donggree.user.internal.application.command;

import java.util.UUID;

/** 인증 코드 원문은 메모리에서만 발송 직전까지 보관한다. */
public record SchoolEmailMailDelivery(Long memberId, UUID requestId, String email, String code) {
    @Override
    public String toString() {
        return "SchoolEmailMailDelivery[requestId=" + requestId + "]";
    }
}
