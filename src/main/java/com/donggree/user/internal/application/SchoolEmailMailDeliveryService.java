package com.donggree.user.internal.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 메모리 상태를 확인하고 전용 워커에서 SMTP 발송을 수행한다. */
@Service
@RequiredArgsConstructor
public class SchoolEmailMailDeliveryService {
    private final SchoolEmailMailQueue queue;
    private final SchoolEmailVerificationStore verifications;
    private final SchoolEmailSender sender;
    private final SchoolEmailTelemetry telemetry;

    public boolean hasWork() {
        return queue.hasWork();
    }

    @Transactional(propagation = Propagation.NEVER)
    public boolean deliverNext() {
        var next = queue.poll();
        if (next.isEmpty()) return false;
        var delivery = next.get();
        // 선점 전에 새 요청으로 교체되거나 탈퇴·인증이 완료된 작업은 발송하지 않는다.
        if (!verifications.startDelivery(delivery.memberId(), delivery.requestId())) {
            telemetry.record(SchoolEmailTelemetry.Action.SKIPPED, delivery, 0, null);
            return true;
        }
        telemetry.record(
                SchoolEmailTelemetry.Action.STARTED,
                delivery,
                Math.max(0, System.nanoTime() - delivery.queuedAtNanos()),
                null);
        long started = System.nanoTime();
        boolean succeeded = false;
        RuntimeException failure = null;
        try {
            sender.send(delivery.email(), delivery.code());
            succeeded = true;
        } catch (RuntimeException exception) {
            failure = exception;
        }
        verifications.completeDelivery(
                delivery.memberId(), delivery.requestId(), succeeded, LocalDateTime.now(ZoneOffset.UTC));
        telemetry.record(
                succeeded ? SchoolEmailTelemetry.Action.SENT : SchoolEmailTelemetry.Action.FAILED,
                delivery,
                System.nanoTime() - started,
                failure);
        return true;
    }
}
