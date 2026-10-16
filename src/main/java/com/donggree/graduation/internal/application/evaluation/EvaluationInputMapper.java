package com.donggree.graduation.internal.application.evaluation;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.graduation.internal.domain.MajorRoleRuleMatcher;
import com.donggree.graduation.internal.domain.evaluation.CourseClassification;
import com.donggree.graduation.internal.domain.evaluation.EvaluationCourse;
import com.donggree.graduation.internal.domain.evaluation.EvaluationTranscript;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 모듈 공개 View를 graduation이 소유하는 판정 입력으로 변환한다. */
@Component
@RequiredArgsConstructor
public class EvaluationInputMapper {
    private final RuleConfigurationReader settingsReader;

    public EvaluationRule rule(GraduationRuleView source) {
        return new EvaluationRule(
                source.id(),
                source.typeName(),
                source.courseType(),
                source.ruleName(),
                settingsReader.read(source.typeName(), source.ruleConfig()),
                "MIN_CREDITS".equals(source.typeName())
                        ? settingsReader.readCreditAdjustments(source.ruleConfig())
                        : List.of());
    }

    public boolean applies(GraduationRuleView source, MajorRole role) {
        List<String> roles = MajorRoleRuleMatcher.supportsMajorRole(source.typeName())
                ? settingsReader.readMajorRoles(source.ruleConfig())
                : List.of();
        return MajorRoleRuleMatcher.applies(source.typeName(), roles, role);
    }

    public static EvaluationTranscript transcript(TranscriptView source) {
        return new EvaluationTranscript(
                source.studentType(),
                source.totalCredits(),
                source.gpa(),
                source.englishLevel(),
                source.englishCourseTarget(),
                source.completedEnglishResult(),
                source.thesisStatus(),
                source.dualMajor1ThesisStatus(),
                source.dualMajor1Id() != null || source.dualMajor2Id() != null,
                source.courseRecords().stream()
                        .map(EvaluationInputMapper::course)
                        .toList(),
                source.majorGpa(),
                source.teachingAptitudeCount(),
                source.teachingCredits(),
                source.teachingGpa());
    }

    public static EvaluationCourse course(CourseRecordView source) {
        return new EvaluationCourse(
                source.semester(),
                source.courseCode(),
                source.courseTypeName(),
                source.pdfAreaName(),
                source.courseName(),
                source.credits(),
                source.passed());
    }

    public static CourseClassification classification(CourseClassificationView source) {
        return new CourseClassification(
                source.courseType(), source.areaName(), source.subCategory(), source.subjectDomain());
    }

    public static Map<String, CourseClassification> classifications(Map<String, CourseClassificationView> sources) {
        return sources.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> classification(entry.getValue())));
    }
}
