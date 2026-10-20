package com.donggree.user.internal.application;

import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.user.internal.application.command.SchoolEmailMailDelivery;
import com.donggree.user.internal.application.exception.SchoolEmailSendException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 발송 관측은 SMTP 접수 결과 기준이다. 주소·코드·예외 원문과 고유값 메트릭 라벨은 사용하지 않는다. */
@Component
public class SchoolEmailTelemetry {
    public enum Action {
        QUEUED,
        REPLACED,
        CANCELLED,
        SKIPPED,
        STARTED,
        SENT,
        FAILED
    }

    private final MeterRegistry registry;
    private final byte[] key;

    public SchoolEmailTelemetry(MeterRegistry registry, @Value("${app.jwt.secret}") String secret) {
        this.registry = registry;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void identify(Long memberId, String email) {
        RequestDiagnostics.emailAttempt(
                pseudonym("member:" + memberId), email == null ? null : pseudonym("email:" + email));
    }

    public void gauge(String name, Supplier<Number> value) {
        try {
            Gauge.builder(name, value, supplier -> supplier.get().doubleValue())
                    .strongReference(true)
                    .register(registry);
        } catch (RuntimeException ignored) {
            // 측정 실패가 큐나 인증 동작을 바꾸지 않는다.
        }
    }

    public void record(Action action, SchoolEmailMailDelivery delivery, long durationNanos, RuntimeException failure) {
        try {
            String reason = failure == null
                    ? "none"
                    : failure instanceof SchoolEmailSendException known
                            ? known.getReason().name().toLowerCase(java.util.Locale.ROOT)
                            : "unexpected";
            String state = action.name().toLowerCase(java.util.Locale.ROOT);
            registry.counter("donggree.mail.events", "action", state, "reason", reason)
                    .increment();
            if (action == Action.SENT || action == Action.FAILED) {
                Timer.builder("donggree.mail.delivery")
                        .tags("outcome", state, "reason", reason)
                        .publishPercentileHistogram()
                        .register(registry)
                        .record(durationNanos, TimeUnit.NANOSECONDS);
            }
            if (action == Action.STARTED) {
                registry.timer("donggree.mail.queue.wait").record(durationNanos, TimeUnit.NANOSECONDS);
            }
            var logger = LoggerFactory.getLogger(SchoolEmailTelemetry.class);
            var event = action == Action.FAILED ? logger.atError() : logger.atInfo();
            event.addKeyValue("event", "school_email_delivery")
                    .addKeyValue("action", state)
                    .addKeyValue("reason", reason)
                    .addKeyValue("delivery_id", delivery.requestId())
                    .addKeyValue("request_id", delivery.originRequestId())
                    .addKeyValue("actor_key", pseudonym("member:" + delivery.memberId()))
                    .addKeyValue("recipient_key", pseudonym("email:" + delivery.email()))
                    .addKeyValue("duration_ms", TimeUnit.NANOSECONDS.toMillis(durationNanos));
            RequestDiagnostics.exceptionFields(event, failure);
            event.log("학교 이메일 발송 상태");
        } catch (RuntimeException ignored) {
            // 개인정보 보호와 업무 처리가 관측 시스템에 의존하지 않도록 한다.
        }
    }

    private String pseudonym(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException | RuntimeException ignored) {
            return null;
        }
    }
}
