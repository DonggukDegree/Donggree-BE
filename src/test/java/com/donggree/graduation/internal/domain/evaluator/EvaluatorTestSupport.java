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
public abstract class EvaluatorTestSupport {

    protected static TranscriptView transcript(int totalCredits, double gpa, List<CourseRecordView> records) {
        return new TranscriptView(
                1L,
                1L,
                100L,
                null,
                null,
                null,
                null,
                2023,
                "단일",
                false,
                totalCredits,
                BigDecimal.valueOf(gpa),
                "S1",
                false,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                records);
    }

    protected static TranscriptView withTeachingSummary(TranscriptView transcript, Integer credits, double gpa) {
        return new TranscriptView(
                transcript.id(),
                transcript.memberId(),
                transcript.departmentId(),
                transcript.dualMajor1Id(),
                transcript.dualMajor2Id(),
                transcript.subMajor1Id(),
                transcript.subMajor2Id(),
                transcript.admissionYear(),
                transcript.studentType(),
                transcript.engineeringCertified(),
                transcript.totalCredits(),
                transcript.gpa(),
                transcript.englishLevel(),
                transcript.englishCourseTarget(),
                transcript.completedEnglishResult(),
                transcript.englishPassResult(),
                transcript.thesisStatus(),
                transcript.dualMajor1ThesisStatus(),
                transcript.transfer(),
                transcript.dualMajorDeclared(),
                transcript.unresolvedAdditionalMajor(),
                transcript.courseRecords(),
                transcript.majorGpa(),
                transcript.teachingAptitudeCount(),
                credits,
                BigDecimal.valueOf(gpa));
    }

    protected static TranscriptView transcriptWith(
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
                null,
                null,
                null,
                null,
                2023,
                studentType,
                false,
                totalCredits,
                BigDecimal.valueOf(gpa),
                englishLevel,
                englishTarget,
                completedEnglish,
                null,
                thesisStatus,
                false,
                false,
                false,
                false,
                records);
    }

    protected static TranscriptView transcriptWithThesisStatuses(
            boolean primaryPassed, boolean secondaryPassed, String studentType, List<CourseRecordView> records) {
        return new TranscriptView(
                1L,
                1L,
                100L,
                200L,
                null,
                null,
                null,
                2023,
                studentType,
                false,
                0,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
                null,
                primaryPassed,
                secondaryPassed,
                false,
                false,
                false,
                records);
    }

    protected static CourseRecordView passed(String code, String name, int credits, String semester) {
        return new CourseRecordView(semester, code, null, null, name, credits, true, false);
    }

    protected static CourseRecordView failed(String code, String name, int credits, String semester) {
        return new CourseRecordView(semester, code, null, null, name, credits, false, false);
    }

    protected static EvaluationContext context(TranscriptView t, Map<String, CourseClassificationView> cls) {
        return new EvaluationContext(t, cls);
    }

    protected static EvaluationContext contextNoClassification(TranscriptView t) {
        return new EvaluationContext(t, Map.of());
    }

    protected static CourseClassificationView classification(
            CourseType type, String areaName, String subCategory, String subjectDomain) {
        return new CourseClassificationView(type, areaName, subCategory, subjectDomain);
    }

    protected static CourseClassificationView classification(CourseType type, String areaName) {
        return new CourseClassificationView(type, areaName, null, null);
    }

    protected static CourseClassificationView classification(CourseType type) {
        return new CourseClassificationView(type, null, null, null);
    }
}
