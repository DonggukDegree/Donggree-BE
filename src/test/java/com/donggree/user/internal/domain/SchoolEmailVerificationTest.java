package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SchoolEmailVerificationTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 18, 12, 0);

    @Test
    void 만료와_재발송_시간의_경계값을_적용한다() {
        var verification = new SchoolEmailVerification(1L);
        verification.issue(SchoolEmailAddress.fromLocalPart("student"), "hash", now);
        assertThat(verification.canResend(now.plusSeconds(59))).isFalse();
        assertThat(verification.canResend(now.plusSeconds(60))).isTrue();
        assertThat(verification.isExpired(now.plusSeconds(299))).isFalse();
        assertThat(verification.isExpired(now.plusSeconds(300))).isTrue();
    }

    @Test
    void 아이디를_바꿔도_발송제한을_유지하고_다음날_일일제한을_초기화한다() {
        var verification = new SchoolEmailVerification(1L);
        for (int index = 0; index < 10; index++) {
            verification.issue(SchoolEmailAddress.fromLocalPart("student" + index), "hash", now.plusMinutes(index));
        }
        var anotherAddress = SchoolEmailAddress.fromLocalPart("another");
        assertThat(verification.reachedDailyLimit(now.plusMinutes(10))).isTrue();
        assertThatThrownBy(() -> verification.issue(anotherAddress, "hash", now.plusMinutes(10)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(verification.getDailySends()).isEqualTo(10);
        verification.issue(SchoolEmailAddress.fromLocalPart("student"), "new-hash", now.plusDays(1));
        assertThat(verification.getDailySends()).isEqualTo(1);
    }
}
