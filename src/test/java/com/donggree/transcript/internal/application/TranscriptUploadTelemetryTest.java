package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class TranscriptUploadTelemetryTest {
    @Test
    void 성공응답의_학점차이와_F확인_과목없음_학과미연결을_구분한다() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new TranscriptUploadTelemetry(registry);
        telemetry.saved(1024, 10, 3, 2, true);
        telemetry.saved(1024, 0, 68, 0, false);
        telemetry.saved(1024, 10, 0, 0, true);
        assertThat(registry.get("donggree.transcript.upload")
                        .tags("quality", "both", "department", "mapped")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("donggree.transcript.upload")
                        .tags("quality", "empty_courses", "department", "unmapped")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("donggree.transcript.upload")
                        .tag("quality", "clean")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
