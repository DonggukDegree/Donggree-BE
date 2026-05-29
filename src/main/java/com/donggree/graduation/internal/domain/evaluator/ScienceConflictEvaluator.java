package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 학문기초 실험·개론 충돌 규칙 평가기.
 * ruleConfig: {} (설정 없음, 규칙이 고정되어 있음)
 *
 * 동일한 subjectDomain의 실험 과목(subCategory="실험")과 개론 과목(subCategory="개론")을
 * 동시에 이수할 수 없다. 충돌이 존재하면 미충족으로 판정한다.
 */
@Component
public class ScienceConflictEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "SCIENCE_CONFLICT";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        List<CourseRecordView> passed = context.getPassedCourses();

        Set<String> experimentDomains = passed.stream()
                .map(cr -> context.getClassification(cr.courseCode()))
                .filter(cls -> cls != null
                        && cls.courseType() == CourseType.ACADEMIC_FOUNDATION
                        && "실험".equals(cls.subCategory())
                        && cls.subjectDomain() != null)
                .map(CourseClassificationView::subjectDomain)
                .collect(Collectors.toSet());

        Set<String> introductionDomains = passed.stream()
                .map(cr -> context.getClassification(cr.courseCode()))
                .filter(cls -> cls != null
                        && cls.courseType() == CourseType.ACADEMIC_FOUNDATION
                        && "개론".equals(cls.subCategory())
                        && cls.subjectDomain() != null)
                .map(CourseClassificationView::subjectDomain)
                .collect(Collectors.toSet());

        boolean hasConflict = experimentDomains.stream().anyMatch(introductionDomains::contains);
        return new RuleResult(rule.ruleName(), !hasConflict);
    }
}
