package com.donggree.transcript.internal.domain.transcript;

import com.donggree.transcript.internal.domain.enums.Grade;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/** 원문 요약과 일치하는 모든 F/NP 조합의 공통 판별만 적용한다. 학수번호별 추정은 하지 않는다. */
public final class FailGradeInference {
    // 비정상적으로 많은 F가 있는 문서도 업로드 스레드를 무제한 점유하지 않는다.
    private static final int MAX_FAILURES = 20;

    private FailGradeInference() {}

    public enum Status {
        RESOLVED,
        AMBIGUOUS,
        GPA_MISMATCH,
        INCOMPLETE_DATA,
        SEARCH_LIMIT
    }

    public record Summary(Integer earnedCredits, BigDecimal gpa, Set<String> courseTypes) {
        boolean includes(CourseRecordData course) {
            return courseTypes.isEmpty() || courseTypes.contains(course.courseTypeName());
        }
    }

    public record Result(List<Grade> grades, List<Integer> reviewIndices, Status status) {}

    public static Result resolve(List<CourseRecordData> courses, Summary total, List<Summary> additional) {
        List<Integer> failures = IntStream.range(0, courses.size())
                .filter(i -> courses.get(i).grade() == Grade.F)
                .boxed()
                .toList();
        List<Grade> original = courses.stream().map(CourseRecordData::grade).toList();
        if (failures.isEmpty()) return new Result(original, List.of(), Status.RESOLVED);
        if (!complete(courses, total)) return new Result(original, failures, Status.INCOMPLETE_DATA);
        if (failures.size() > MAX_FAILURES) return new Result(original, failures, Status.SEARCH_LIMIT);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(courses, failures, total));
        // 영역 요약은 그 영역의 취득학점도 맞을 때만 보조 근거로 사용한다.
        for (Summary summary : additional) {
            if (complete(courses, summary)) constraints.add(new Constraint(courses, failures, summary));
        }
        Search search = new Search(failures.size(), constraints);
        search.visit(0, 0);
        if (search.matches == 0) return new Result(original, failures, Status.GPA_MISMATCH);

        List<Grade> grades = new ArrayList<>(original);
        List<Integer> review = new ArrayList<>();
        for (int bit = 0; bit < failures.size(); bit++) {
            int index = failures.get(bit);
            if ((search.alwaysExcluded & (1 << bit)) != 0) grades.set(index, Grade.NP);
            else if ((search.sometimesExcluded & (1 << bit)) != 0) review.add(index);
        }
        return new Result(
                List.copyOf(grades), List.copyOf(review), review.isEmpty() ? Status.RESOLVED : Status.AMBIGUOUS);
    }

    private static boolean complete(List<CourseRecordData> courses, Summary summary) {
        if (summary.earnedCredits() == null
                || summary.gpa() == null
                || summary.gpa().signum() < 0
                || summary.gpa().compareTo(new BigDecimal("4.50")) > 0
                || summary.gpa().stripTrailingZeros().scale() > 2) return false;
        long earned = courses.stream()
                .filter(summary::includes)
                .filter(c -> c.grade() != Grade.F && c.grade() != Grade.NP)
                .mapToLong(CourseRecordData::credits)
                .sum();
        return earned == summary.earnedCredits();
    }

    private static final class Constraint {
        private final long weightedTwice;
        private final int expectedHundredths;
        private final int[] failedCredits;
        private long denominator;

        Constraint(List<CourseRecordData> courses, List<Integer> failures, Summary summary) {
            long weighted = 0;
            for (CourseRecordData course : courses) {
                if (!summary.includes(course) || course.grade() == Grade.P || course.grade() == Grade.NP) continue;
                denominator += course.credits();
                weighted += course.grade()
                                .getGradePoint()
                                .multiply(BigDecimal.valueOf(2))
                                .longValueExact()
                        * course.credits();
            }
            weightedTwice = weighted;
            expectedHundredths = summary.gpa().movePointRight(2).intValueExact();
            failedCredits = failures.stream()
                    .map(courses::get)
                    .mapToInt(c -> summary.includes(c) ? c.credits() : 0)
                    .toArray();
        }

        boolean matches() {
            // 학점이 정수이고 평점은 0.5 단위이므로, 부동소수점 오차 없이 HALF_UP 소수 둘째 자리 비교.
            long rounded = denominator == 0 ? 0 : (weightedTwice * 100 + denominator) / (2 * denominator);
            return rounded == expectedHundredths;
        }
    }

    private static final class Search {
        private final int size;
        private final List<Constraint> constraints;
        private int matches;
        private int alwaysExcluded;
        private int sometimesExcluded;

        Search(int size, List<Constraint> constraints) {
            this.size = size;
            this.constraints = constraints;
            alwaysExcluded = (1 << size) - 1;
        }

        void visit(int bit, int mask) {
            if (bit == size) {
                for (Constraint constraint : constraints) if (!constraint.matches()) return;
                matches++;
                alwaysExcluded &= mask;
                sometimesExcluded |= mask;
                return;
            }
            visit(bit + 1, mask);
            for (Constraint constraint : constraints) constraint.denominator -= constraint.failedCredits[bit];
            visit(bit + 1, mask | (1 << bit));
            for (Constraint constraint : constraints) constraint.denominator += constraint.failedCredits[bit];
        }
    }
}
