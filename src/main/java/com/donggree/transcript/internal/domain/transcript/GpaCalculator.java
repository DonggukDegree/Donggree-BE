package com.donggree.transcript.internal.domain.transcript;

import com.donggree.transcript.internal.domain.enums.Grade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** 저장 수강 이력과 PDF 미리보기에 동일한 평점 가중평균·반올림 기준을 적용한다. */
public final class GpaCalculator {
    private GpaCalculator() {}

    /** P·NP는 제외하고 F는 분모에 포함한다. 평가 가능한 학점이 없으면 null을 반환한다. */
    public static <T> BigDecimal calculate(List<T> records, ToIntFunction<T> creditsOf, Function<T, Grade> gradeOf) {
        return calculate(records, creditsOf, gradeOf, record -> false);
    }

    public static <T> BigDecimal calculate(
            List<T> records,
            ToIntFunction<T> creditsOf,
            Function<T, Grade> gradeOf,
            java.util.function.Predicate<T> passFailCourse) {
        int denominator = 0;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (T record : records) {
            Grade grade = gradeOf.apply(record);
            if (passFailCourse.test(record) || grade == Grade.P || grade == Grade.NP) continue;
            int credits = creditsOf.applyAsInt(record);
            denominator += credits;
            weightedSum = weightedSum.add(grade.getGradePoint().multiply(BigDecimal.valueOf(credits)));
        }
        return denominator == 0 ? null : weightedSum.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
