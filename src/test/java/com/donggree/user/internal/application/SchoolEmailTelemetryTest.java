package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.donggree.user.internal.application.command.SchoolEmailMailDelivery;
import com.donggree.user.internal.application.exception.SchoolEmailSendException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SchoolEmailTelemetryTest {
    @Test
    void SMTP_실패를_화면조회와_독립적으로_한번_집계하고_원문을_제외한다() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new SchoolEmailTelemetry(registry, "local-test-key");
        var queue = mock(SchoolEmailMailQueue.class);
        var store = mock(SchoolEmailVerificationStore.class);
        var sender = mock(SchoolEmailSender.class);
        var delivery = new SchoolEmailMailDelivery(
                77L, UUID.randomUUID(), "private@dgu.ac.kr", "654321", "origin-request", System.nanoTime());
        when(queue.poll()).thenReturn(Optional.of(delivery));
        when(store.startDelivery(77L, delivery.requestId())).thenReturn(true);
        doThrow(new SchoolEmailSendException(SchoolEmailSendException.Reason.TIMEOUT))
                .when(sender)
                .send(anyString(), anyString());
        var logger = (Logger) LoggerFactory.getLogger(SchoolEmailTelemetry.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(new SchoolEmailMailDeliveryService(queue, store, sender, telemetry).deliverNext())
                    .isTrue();
            verify(store).completeDelivery(eq(77L), eq(delivery.requestId()), eq(false), any(LocalDateTime.class));
            assertThat(registry.get("donggree.mail.events")
                            .tags("action", "failed", "reason", "timeout")
                            .counter()
                            .count())
                    .isEqualTo(1);
            assertThat(registry.get("donggree.mail.delivery")
                            .tags("outcome", "failed")
                            .timer()
                            .count())
                    .isEqualTo(1);
            String fields = appender.list.toString()
                    + appender.list.stream()
                            .map(e -> e.getKeyValuePairs().toString())
                            .toList();
            assertThat(fields)
                    .contains(
                            "origin-request", delivery.requestId().toString(), "actor_key", "recipient_key", "timeout")
                    .doesNotContain("private@dgu.ac.kr", "654321", "memberId=77");
            assertThat(registry.getMeters().toString())
                    .doesNotContain("private@", "654321", delivery.requestId().toString());
        } finally {
            logger.detachAppender(appender);
            registry.close();
        }
    }

    @Test
    void 오래된_작업은_발송성공으로_집계하지_않는다() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new SchoolEmailTelemetry(registry, "test-key");
        var queue = mock(SchoolEmailMailQueue.class);
        var store = mock(SchoolEmailVerificationStore.class);
        var sender = mock(SchoolEmailSender.class);
        var delivery = new SchoolEmailMailDelivery(1L, UUID.randomUUID(), "student@dgu.ac.kr", "012345");
        when(queue.poll()).thenReturn(Optional.of(delivery));
        new SchoolEmailMailDeliveryService(queue, store, sender, telemetry).deliverNext();
        verifyNoInteractions(sender);
        assertThat(registry.get("donggree.mail.events")
                        .tag("action", "skipped")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.find("donggree.mail.delivery").timer()).isNull();
    }
}
