package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClubProfileTest {

    private static final ClubProfile BASE = ClubFixtures.profile("소리마루");

    @Test
    void 앞뒤_공백을_정리하고_중복_분야를_합친다() {
        ClubProfile profile = new ClubProfile(
                "  소리마루 ",
                BASE.affiliation(),
                List.of(ClubField.PERFORMANCE, ClubField.PERFORMANCE, ClubField.ACADEMIC),
                "  " + BASE.description() + "  ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(profile.name()).isEqualTo("소리마루");
        assertThat(profile.fields()).containsExactly(ClubField.PERFORMANCE, ClubField.ACADEMIC);
        assertThat(profile.description()).isEqualTo(BASE.description().strip());
        assertThat(profile.officers()).isEmpty();
        assertThat(profile.links()).isEmpty();
    }

    @Test
    void 소개는_100자_이상이어야_한다() {
        assertThatThrownBy(() -> withDescription("짧은 소개")).isInstanceOf(IllegalArgumentException.class);
        assertThat(withDescription("가".repeat(100)).description()).hasSize(100);
        assertThatThrownBy(() -> withDescription("가".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 활동_분야는_1개에서_3개다() {
        assertThatThrownBy(() -> withFields(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withFields(
                        List.of(ClubField.ACADEMIC, ClubField.BEAUTY, ClubField.EDUCATION, ClubField.VOLUNTEER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기수제는_활동_기간이_필요하고_기수가_없으면_기간을_두지_않는다() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        assertThatThrownBy(() -> withGeneration(24, start, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withGeneration(24, start, start.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withGeneration(null, start, start)).isInstanceOf(IllegalArgumentException.class);
        assertThat(withGeneration(24, start, start).generation()).isEqualTo(24);
    }

    @Test
    void 운영진과_링크는_입력_규칙을_지킨다() {
        assertThatThrownBy(() -> new ClubOfficer("회장", "", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ClubOfficer("회장", null, "김동글").department()).isEmpty();
        assertThatThrownBy(() -> new ClubLink("홈", "http://donggree.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClubLink("공식 홈페이지 바로가기", "https://donggree.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClubLink("홈", "javascript:alert(1)")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 등록하면_정보를_담고_임시_등록만_최초_등록_포인트_대상이다() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 27, 10, 0);

        Club provisional = Club.register(BASE, ClubRegistration.PROVISIONAL, now);
        Club official = Club.register(BASE, ClubRegistration.OFFICIAL, now);

        assertThat(provisional.getName()).isEqualTo("소리마루");
        assertThat(provisional.sortedFields()).containsExactly(ClubField.PERFORMANCE);
        assertThat(provisional.getOfficers()).hasSize(1);
        assertThat(provisional.getInfoUpdatedAt()).isEqualTo(now);
        assertThat(provisional.firstRegistrationRewardAvailable()).isTrue();
        assertThat(official.firstRegistrationRewardAvailable()).isFalse();
    }

    private static ClubProfile withDescription(String description) {
        return new ClubProfile(
                BASE.name(),
                BASE.affiliation(),
                BASE.fields(),
                description,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static ClubProfile withFields(List<ClubField> fields) {
        return new ClubProfile(
                BASE.name(),
                BASE.affiliation(),
                fields,
                BASE.description(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static ClubProfile withGeneration(Integer generation, LocalDate start, LocalDate end) {
        return new ClubProfile(
                BASE.name(),
                BASE.affiliation(),
                BASE.fields(),
                BASE.description(),
                generation,
                start,
                end,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
