package com.donggree.curriculum.internal.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequirementTrackTest {

    @Test
    void 공학인증심화대상이면_심화과정으로_판별한다() {
        assertThat(RequirementTrack.ofStudent(true)).isEqualTo(RequirementTrack.ADVANCED);
        assertThat(RequirementTrack.ofStudent(false)).isEqualTo(RequirementTrack.GENERAL);
    }

    @Test
    void ALL_세트는_모든_과정의_학생을_받는다() {
        assertThat(RequirementTrack.ALL.covers(RequirementTrack.GENERAL)).isTrue();
        assertThat(RequirementTrack.ALL.covers(RequirementTrack.ADVANCED)).isTrue();
    }

    @Test
    void 일반과정과_심화과정_세트는_서로의_학생을_받지_않는다() {
        assertThat(RequirementTrack.GENERAL.covers(RequirementTrack.GENERAL)).isTrue();
        assertThat(RequirementTrack.GENERAL.covers(RequirementTrack.ADVANCED)).isFalse();

        assertThat(RequirementTrack.ADVANCED.covers(RequirementTrack.ADVANCED)).isTrue();
        assertThat(RequirementTrack.ADVANCED.covers(RequirementTrack.GENERAL)).isFalse();
    }

    @Test
    void ALL_세트는_모든_과정과_겹친다() {
        assertThat(RequirementTrack.ALL.conflictingTracks())
                .containsExactlyInAnyOrder(RequirementTrack.ALL, RequirementTrack.GENERAL, RequirementTrack.ADVANCED);
    }

    @Test
    void 일반과정_세트는_심화과정_세트와_겹치지_않는다() {
        // 같은 학과·같은 적용년도에 일반과정 세트와 심화과정 세트를 함께 둘 수 있어야 한다.
        assertThat(RequirementTrack.GENERAL.conflictingTracks())
                .containsExactlyInAnyOrder(RequirementTrack.ALL, RequirementTrack.GENERAL)
                .doesNotContain(RequirementTrack.ADVANCED);

        assertThat(RequirementTrack.ADVANCED.conflictingTracks())
                .containsExactlyInAnyOrder(RequirementTrack.ALL, RequirementTrack.ADVANCED)
                .doesNotContain(RequirementTrack.GENERAL);
    }
}
