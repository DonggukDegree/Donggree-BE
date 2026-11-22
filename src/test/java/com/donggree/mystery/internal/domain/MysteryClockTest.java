package com.donggree.mystery.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.mystery.internal.infrastructure.MysteryClock;
import java.time.*;
import org.junit.jupiter.api.Test;

class MysteryClockTest {
    @Test
    void 한국의_자정은_utc_15시에_바뀐다() {
        var before = new MysteryClock() {
            @Override
            public Instant now() {
                return Instant.parse("2026-11-19T14:59:59Z");
            }
        };
        var after = new MysteryClock() {
            @Override
            public Instant now() {
                return Instant.parse("2026-11-19T15:00:00Z");
            }
        };
        assertThat(before.today()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(after.today()).isEqualTo(LocalDate.of(2026, 11, 20));
    }
}
