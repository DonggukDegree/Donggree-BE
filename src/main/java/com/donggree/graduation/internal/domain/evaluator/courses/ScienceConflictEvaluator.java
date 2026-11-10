package com.donggree.graduation.internal.domain.evaluator.courses;

import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluation.CourseClassification;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.evaluation.EvaluationCourse;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 학문기초 실험·개론 충돌 규칙 평가기.
 * ruleConfig: {} (설정 없음, 규칙이 고정되어 있음)
 *
 * 동일한 subjectDomain의 실험 과목(subCategory="실험")과 개론 과목(subCategory="개론")을
 * 동시에 이수할 수 없다. 충돌이 존재하면 미충족으로 판정한다.
 */
public class ScienceConflictEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "SCIENCE_CONFLICT";
    }

    @Override
    public RuleResult evaluate(EvaluationRule rule, EvaluationContext context) {
        List<EvaluationCourse> passed = context.getPassedCourses();

        Set<String> experimentDomains = passed.stream()
                .map(cr -> context.getClassification(cr.courseCode()))
                .filter(cls -> cls != null
                        && cls.courseType() == CourseType.ACADEMIC_FOUNDATION
                        && "실험".equals(cls.subCategory())
                        && cls.subjectDomain() != null)
                .map(CourseClassification::subjectDomain)
                .collect(Collectors.toSet());

        Set<String> introductionDomains = passed.stream()
                .map(cr -> context.getClassification(cr.courseCode()))
                .filter(cls -> cls != null
                        && cls.courseType() == CourseType.ACADEMIC_FOUNDATION
                        && "개론".equals(cls.subCategory())
                        && cls.subjectDomain() != null)
                .map(CourseClassification::subjectDomain)
                .collect(Collectors.toSet());

        boolean hasConflict = experimentDomains.stream().anyMatch(introductionDomains::contains);
        return new RuleResult(rule.ruleName(), !hasConflict);
    }
}
