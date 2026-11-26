package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ClubFixtures {

    public static final String DESCRIPTION = "매주 합주하고 학기마다 정기 공연을 여는 밴드 동아리예요. ".repeat(4);

    private ClubFixtures() {}

    /** 기수제(24기, 2026-03-01~2027-02-28) 중앙동아리. */
    public static ClubProfile profile(String name) {
        return new ClubProfile(
                name,
                ClubAffiliation.CENTRAL,
                List.of(ClubField.PERFORMANCE),
                DESCRIPTION,
                24,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 2, 28),
                "학생회관 312호",
                14,
                120,
                LocalDate.of(1998, 3, 2),
                List.of(new ClubOfficer("회장", "컴퓨터공학과", "김동글")),
                List.of(new ClubLink("인스타그램", "https://instagram.com/donggree")));
    }

    public static RecruitmentPlan period(LocalDateTime startsAt, LocalDateTime endsAt) {
        return new RecruitmentPlan(
                25,
                new RecruitmentSchedule(false, startsAt, endsAt, false),
                List.of(new ClubLink("지원서", "https://forms.gle/donggree")),
                "면접은 3월 둘째 주예요.");
    }

    public static RecruitmentPlan alwaysOpen() {
        return new RecruitmentPlan(null, new RecruitmentSchedule(true, null, null, false), List.of(), null);
    }
}
