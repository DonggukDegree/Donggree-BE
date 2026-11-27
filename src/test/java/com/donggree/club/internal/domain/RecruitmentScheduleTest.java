package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RecruitmentScheduleTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 2, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 3, 9, 23, 59);
    private static final RecruitmentSchedule PERIOD = new RecruitmentSchedule(false, START, END, false);

    @Test
    void 기간_모집은_시작_포함_마감_제외로_열린다() {
        assertThat(PERIOD.statusAt(START.minusSeconds(1))).isEqualTo(RecruitmentStatus.BEFORE);
        assertThat(PERIOD.statusAt(START)).isEqualTo(RecruitmentStatus.OPEN);
        assertThat(PERIOD.statusAt(END)).isEqualTo(RecruitmentStatus.CLOSED);
        assertThat(PERIOD.openDeadlineAt(START)).isEqualTo(END);
        assertThat(PERIOD.openDeadlineAt(END)).isNull();
    }

    @Test
    void 조기_마감은_기간보다_우선하고_상시모집은_마감_전까지_열려_있다() {
        assertThat(new RecruitmentSchedule(false, START, END, true).statusAt(START))
                .isEqualTo(RecruitmentStatus.CLOSED);
        RecruitmentSchedule always = new RecruitmentSchedule(true, null, null, false);
        assertThat(always.statusAt(START)).isEqualTo(RecruitmentStatus.OPEN);
        assertThat(always.openDeadlineAt(START)).isNull();
        assertThat(new RecruitmentSchedule(true, null, null, true).statusAt(START))
                .isEqualTo(RecruitmentStatus.CLOSED);
    }

    @Test
    void 기간_모집은_시작과_마감이_필요하고_상시모집은_기간을_두지_않는다() {
        assertThatThrownBy(() -> new RecruitmentSchedule(false, START, null, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitmentSchedule(false, END, START, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitmentSchedule(true, START, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 마감됐고_기수_활동도_끝났으면_현재_모집_없이_지난_모집으로_보여준다() {
        String latest = "24기 모집";
        LocalDate activityEnd = LocalDate.of(2027, 2, 28);

        RecruitmentSelection<String> during =
                RecruitmentSelection.of(latest, PERIOD, activityEnd, LocalDateTime.of(2026, 5, 1, 0, 0));
        RecruitmentSelection<String> after =
                RecruitmentSelection.of(latest, PERIOD, activityEnd, LocalDateTime.of(2027, 3, 1, 0, 0));
        RecruitmentSelection<String> noGeneration =
                RecruitmentSelection.of(latest, PERIOD, null, LocalDateTime.of(2027, 3, 1, 0, 0));

        assertThat(during).isEqualTo(new RecruitmentSelection<>(latest, null));
        assertThat(after).isEqualTo(new RecruitmentSelection<>(null, latest));
        assertThat(noGeneration).isEqualTo(new RecruitmentSelection<>(latest, null));
        assertThat(RecruitmentSelection.of(null, null, activityEnd, START))
                .isEqualTo(new RecruitmentSelection<>(null, null));
    }
}
