package com.donggree.graduation.support;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.evaluation.EvaluationInputMapper;
import com.donggree.graduation.internal.application.report.GraduationReportAssembler;
import com.donggree.graduation.internal.domain.CreditAdjustmentCalculator;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import com.donggree.graduation.internal.infrastructure.rule.JacksonReportSettingsReader;
import com.donggree.graduation.internal.infrastructure.rule.JacksonRuleConfigurationReader;
import com.donggree.transcript.TranscriptView;
import java.util.Map;

/** 기존 경계 픽스처를 재사용하는 회귀 테스트용 조립. 운영 코드의 의존성에 포함되지 않는다. */
public final class EvaluationTestInputs {
    private static final JacksonRuleConfigurationReader READER = new JacksonRuleConfigurationReader();

    private EvaluationTestInputs() {}

    public static EvaluationInputMapper mapper() {
        return new EvaluationInputMapper(READER);
    }

    public static GraduationReportAssembler assembler() {
        return new GraduationReportAssembler(READER, new JacksonReportSettingsReader());
    }

    public static EvaluationRule rule(GraduationRuleView source) {
        return mapper().rule(source);
    }

    public static EvaluationContext context(
            TranscriptView source, Map<String, CourseClassificationView> classifications) {
        return new EvaluationContext(
                EvaluationInputMapper.transcript(source), EvaluationInputMapper.classifications(classifications));
    }

    public static EvaluationContext primary(
            TranscriptView source, Map<String, CourseClassificationView> classifications, MajorRole role) {
        return EvaluationContext.primary(
                EvaluationInputMapper.transcript(source), EvaluationInputMapper.classifications(classifications), role);
    }

    public static EvaluationContext secondary(
            TranscriptView source, Map<String, CourseClassificationView> classifications, String courseTypeName) {
        return EvaluationContext.secondary(
                EvaluationInputMapper.transcript(source),
                EvaluationInputMapper.classifications(classifications),
                courseTypeName);
    }

    public static EvaluationContext report(
            TranscriptView source, Map<String, CourseClassificationView> classifications) {
        return EvaluationContext.report(
                EvaluationInputMapper.transcript(source), EvaluationInputMapper.classifications(classifications));
    }

    public static boolean applies(GraduationRuleView source, MajorRole role) {
        return mapper().applies(source, role);
    }

    public static int adjustedTarget(GraduationRuleView source, EvaluationContext context, int target) {
        return CreditAdjustmentCalculator.adjustedTarget(
                source.courseType(), READER.readCreditAdjustments(source.ruleConfig()), context, target);
    }
}
