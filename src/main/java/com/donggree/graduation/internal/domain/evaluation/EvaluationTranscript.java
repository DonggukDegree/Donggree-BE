package com.donggree.graduation.internal.domain.evaluation;

import java.math.BigDecimal;
import java.util.List;

/** 학번·회원·학과 식별자를 제외한 졸업 판정 입력. */
public record EvaluationTranscript(
        String studentType,
        int totalCredits,
        BigDecimal gpa,
        String englishLevel,
        boolean englishCourseTarget,
        Boolean completedEnglishResult,
        boolean thesisStatus,
        boolean dualMajor1ThesisStatus,
        boolean hasResolvedDualMajor,
        List<EvaluationCourse> courseRecords,
        BigDecimal majorGpa,
        Integer teachingAptitudeCount,
        Integer teachingCredits,
        BigDecimal teachingGpa) {
    public EvaluationTranscript {
        courseRecords = List.copyOf(courseRecords);
    }
}
