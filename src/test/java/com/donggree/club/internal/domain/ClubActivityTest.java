package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClubActivityTest {

    @Test
    void 기수제는_오늘이_활동_기간_안이면_활동_중이다() {
        ClubActivity activity = new ClubActivity(
                24, LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 28), LocalDateTime.of(2020, 1, 1, 0, 0));

        assertThat(activity.confirmedOn(LocalDate.of(2026, 3, 1))).isTrue();
        assertThat(activity.confirmedOn(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(activity.confirmedOn(LocalDate.of(2027, 3, 1))).isFalse();
    }

    @Test
    void 기수가_없으면_최근_3월_9월_확인_시점_이후_정보가_갱신돼야_활동_중이다() {
        ClubActivity updatedInSeptember = new ClubActivity(null, null, null, LocalDateTime.of(2026, 9, 1, 0, 0));

        assertThat(updatedInSeptember.confirmedOn(LocalDate.of(2026, 11, 27))).isTrue();
        assertThat(updatedInSeptember.confirmedOn(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(updatedInSeptember.confirmedOn(LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(ClubActivity.latestCheckOn(LocalDate.of(2026, 2, 10))).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(ClubActivity.latestCheckOn(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2026, 3, 1));
    }
}
