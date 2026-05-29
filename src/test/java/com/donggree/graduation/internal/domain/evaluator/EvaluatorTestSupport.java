package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 평가기 단위 테스트에서 공통으로 사용하는 헬퍼 메서드 모음. */
class EvaluatorTestSupport {

    static TranscriptView transcript(int totalCredits, double gpa, List<CourseRecordView> records) {
        return new TranscriptView(
                1L, 1L, 100L, 2023, "단일", totalCredits, BigDecimal.valueOf(gpa), "S1", false, null, false, records);
    }

    static TranscriptView transcriptWith(
            int totalCredits,
            double gpa,
            boolean englishTarget,
            Boolean completedEnglish,
            boolean thesisStatus,
            String studentType,
            String englishLevel,
            List<CourseRecordView> records) {
        return new TranscriptView(
                1L,
                1L,
                100L,
                2023,
                studentType,
                totalCredits,
                BigDecimal.valueOf(gpa),
                englishLevel,
                englishTarget,
                completedEnglish,
                thesisStatus,
                records);
    }

    static CourseRecordView passed(String code, String name, int credits, String semester) {
        return new CourseRecordView(semester, code, name, credits, true, false);
    }

    static CourseRecordView failed(String code, String name, int credits, String semester) {
        return new CourseRecordView(semester, code, name, credits, false, false);
    }

    static EvaluationContext context(TranscriptView t, Map<String, CourseClassificationView> cls) {
        return new EvaluationContext(t, cls);
    }

    static EvaluationContext contextNoClassification(TranscriptView t) {
        return new EvaluationContext(t, Map.of());
    }

    static CourseClassificationView classification(
            Long courseId, CourseType type, String subCategory, String subjectDomain) {
        return new CourseClassificationView(courseId, type, null, subCategory, subjectDomain);
    }

    static CourseClassificationView classification(Long courseId, CourseType type) {
        return new CourseClassificationView(courseId, type, null, null, null);
    }
}
