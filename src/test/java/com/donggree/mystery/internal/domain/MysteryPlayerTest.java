package com.donggree.mystery.internal.domain;

import static org.assertj.core.api.Assertions.*;

import com.donggree.mystery.internal.domain.MysteryData.*;
import com.donggree.mystery.internal.infrastructure.MysteryCatalog;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MysteryPlayerTest {
    private final LocalDate day = LocalDate.of(2026, 11, 19);

    private void win(MysteryPlayer p, LocalDate date) {
        var data = new MysteryCatalog().generate();
        p.openDay(date);
        p.start(data);
        p.record(new Attempt(UUID.randomUUID(), data.answer(), true, null));
    }

    @Test
    void 실패해도_연속참여를_유지하고_통계에_포함한다() {
        var p = new MysteryPlayer();
        win(p, day);
        p.openDay(day.plusDays(1));
        var data = new MysteryCatalog().generate();
        p.start(data);
        for (int i = 0; i < 10; i++) p.record(new Attempt(UUID.randomUUID(), new Guess("x", "y", "z"), false, null));
        assertThat(p.getStatus()).isEqualTo(Status.LOST);
        assertThat(p.getCurrentStreak()).isEqualTo(2);
        assertThat(p.getCompletedGames()).isEqualTo(2);
        assertThat(p.getWonGames()).isEqualTo(1);
        assertThat(p.getTotalAttempts()).isEqualTo(11);
    }

    @Test
    void 하루를_건너뛰면_초기화되고_그날_완료하면_1일이다() {
        var p = new MysteryPlayer();
        win(p, day);
        p.openDay(day.plusDays(2));
        assertThat(p.getCurrentStreak()).isZero();
        assertThat(p.getCompletedGames()).isZero();
        assertThat(p.tier()).isEqualTo("BRONZE");
        var data = new MysteryCatalog().generate();
        p.start(data);
        assertThat(p.getCurrentStreak()).isZero();
        p.record(new Attempt(UUID.randomUUID(), data.answer(), true, null));
        assertThat(p.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void 미완료로_자정을_넘기면_스트릭과_통계를_초기화한다() {
        var p = new MysteryPlayer();
        win(p, day);
        p.openDay(day.plusDays(1));
        p.start(new MysteryCatalog().generate());
        p.record(new Attempt(UUID.randomUUID(), new Guess("x", "y", "z"), false, null));
        assertThat(p.getCurrentStreak()).isEqualTo(1);
        p.openDay(day.plusDays(2));
        assertThat(p.getCurrentStreak()).isZero();
        assertThat(p.getCompletedGames()).isZero();
        assertThat(p.getWonGames()).isZero();
        assertThat(p.getTotalAttempts()).isZero();
        assertThat(p.tier()).isEqualTo("BRONZE");
    }

    @Test
    void 진행중에는_스트릭과_등급을_올리지_않고_완료할_때_한번만_올린다() {
        var p = new MysteryPlayer();
        for (int i = 0; i < 6; i++) win(p, day.plusDays(i));
        p.openDay(day.plusDays(6));
        var data = new MysteryCatalog().generate();
        p.start(data);
        p.record(new Attempt(UUID.randomUUID(), new Guess("x", "y", "z"), false, null));
        assertThat(p.getCurrentStreak()).isEqualTo(6);
        assertThat(p.tier()).isEqualTo("BRONZE");
        assertThat(p.getCompletedGames()).isEqualTo(6);
        p.record(new Attempt(UUID.randomUUID(), data.answer(), true, null));
        assertThat(p.getCurrentStreak()).isEqualTo(7);
        assertThat(p.tier()).isEqualTo("SILVER");
        assertThat(p.getCompletedGames()).isEqualTo(7);
        assertThatThrownBy(() -> p.record(new Attempt(UUID.randomUUID(), data.answer(), true, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(p.getCurrentStreak()).isEqualTo(7);
    }

    @Test
    void 연꽃은_7일_14일_30일에_승급한다() {
        var p = new MysteryPlayer();
        for (int i = 1; i <= 30; i++) {
            win(p, day.plusDays(i));
            assertThat(p.tier()).isEqualTo(i >= 30 ? "DIAMOND" : i >= 14 ? "GOLD" : i >= 7 ? "SILVER" : "BRONZE");
        }
    }
}
