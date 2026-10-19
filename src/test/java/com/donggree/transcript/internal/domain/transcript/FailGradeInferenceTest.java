package com.donggree.transcript.internal.domain.transcript;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.transcript.internal.domain.enums.Grade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FailGradeInferenceTest {
    private CourseRecordData course(String type, int credits, Grade grade) {
        return new CourseRecordData("2026-1", type, null, "777700", "교류과목", credits, grade, false);
    }

    private FailGradeInference.Summary summary(int credits, String gpa, String... types) {
        return new FailGradeInference.Summary(credits, new BigDecimal(gpa), Set.of(types));
    }

    @Test
    void 단일해에서는_같은_코드라도_행마다_일반_F와_NP로_구분한다() {
        var result = FailGradeInference.resolve(
                List.of(course("전공", 3, Grade.A_ZERO), course("공교", 1, Grade.F), course("일교", 2, Grade.F)),
                summary(3, "3.00"),
                List.of());
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.F, Grade.NP);
        assertThat(result.reviewIndices()).isEmpty();
    }

    @Test
    void 같은_학점의_여러해는_F로_보존하고_모두_확인대상이다() {
        var result = FailGradeInference.resolve(
                List.of(course("전공", 3, Grade.A_ZERO), course("공교", 1, Grade.F), course("일교", 1, Grade.F)),
                summary(3, "3.00"),
                List.of());
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.F, Grade.F);
        assertThat(result.reviewIndices()).containsExactly(1, 2);
    }

    @Test
    void 여러해에서도_모든해가_동의하는_과목만_변경한다() {
        var result = FailGradeInference.resolve(
                List.of(
                        course("전공", 3, Grade.A_ZERO),
                        course("공교", 1, Grade.F),
                        course("일교", 1, Grade.F),
                        course("일교", 4, Grade.F)),
                summary(3, "3.00"),
                List.of());
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.F, Grade.F, Grade.NP);
        assertThat(result.reviewIndices()).containsExactly(1, 2);
    }

    @Test
    void 전공_요약이_일치하면_총평점의_모호함을_해소한다() {
        var result = FailGradeInference.resolve(
                List.of(course("전필", 3, Grade.A_ZERO), course("전선", 1, Grade.F), course("일교", 1, Grade.F)),
                summary(3, "3.00"),
                List.of(summary(3, "4.00", "전공", "전필", "전선")));
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.NP, Grade.F);
        assertThat(result.reviewIndices()).isEmpty();
    }

    @Test
    void 일치하는_조합이_없으면_보정하지_않는다() {
        var result = FailGradeInference.resolve(
                List.of(course("전공", 3, Grade.A_ZERO), course("공교", 1, Grade.F)), summary(3, "3.20"), List.of());
        assertThat(result.status()).isEqualTo(FailGradeInference.Status.GPA_MISMATCH);
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.F);
        assertThat(result.reviewIndices()).containsExactly(1);
    }

    @Test
    void 취득학점이_틀리면_평점이_맞아도_추정하지_않는다() {
        var result = FailGradeInference.resolve(
                List.of(course("전공", 3, Grade.A_ZERO), course("공교", 1, Grade.F)), summary(6, "4.00"), List.of());
        assertThat(result.status()).isEqualTo(FailGradeInference.Status.INCOMPLETE_DATA);
        assertThat(result.reviewIndices()).containsExactly(1);
    }

    @Test
    void P와_기존_NP는_평점에서_제외하고_변경하지_않는다() {
        var result = FailGradeInference.resolve(
                List.of(
                        course("전공", 3, Grade.A_ZERO),
                        course("공교", 2, Grade.P),
                        course("공교", 2, Grade.NP),
                        course("공교", 1, Grade.F)),
                summary(5, "4.00"),
                List.of());
        assertThat(result.grades()).containsExactly(Grade.A_ZERO, Grade.P, Grade.NP, Grade.NP);
    }

    @Test
    void 영학점_F와_영점뿐인_성적은_평점으로_구분할_수_없다() {
        for (int credits : new int[] {0, 1}) {
            var result =
                    FailGradeInference.resolve(List.of(course("공교", credits, Grade.F)), summary(0, "0.00"), List.of());
            assertThat(result.reviewIndices()).containsExactly(0);
            assertThat(result.grades()).containsExactly(Grade.F);
        }
    }

    @Test
    void 반올림은_학교의_소수둘째자리_HALF_UP_기준이다() {
        var result = FailGradeInference.resolve(
                List.of(course("전공", 3, Grade.A_PLUS), course("전공", 4, Grade.B_ZERO), course("공교", 1, Grade.F)),
                summary(7, "3.64"),
                List.of());
        assertThat(result.grades()).containsExactly(Grade.A_PLUS, Grade.B_ZERO, Grade.NP);
        assertThat(result.reviewIndices()).isEmpty();
    }

    @Test
    void 탐색상한을_넘으면_전부_확인대상으로_돌려준다() {
        var result = FailGradeInference.resolve(
                Stream.concat(
                                Stream.of(course("전공", 3, Grade.A_ZERO)),
                                Stream.generate(() -> course("공교", 1, Grade.F)).limit(21))
                        .toList(),
                summary(3, "4.00"),
                List.of());
        assertThat(result.status()).isEqualTo(FailGradeInference.Status.SEARCH_LIMIT);
        assertThat(result.reviewIndices()).hasSize(21);
    }
}
