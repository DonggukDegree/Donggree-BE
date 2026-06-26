package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import org.springframework.stereotype.Component;

/**
 * 학문기초 실험 과목 필수 선택 규칙 평가기.
 * ruleConfig: {"minCount": 1}
 * ACADEMIC_FOUNDATION 분류에서 subCategory = "실험"인 과목을 minCount개 이상 이수하면 충족이다.
 */
@Component
public class ScienceExperimentEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "SCIENCE_EXPERIMENT";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);

        long count = context.getPassedCourses().stream()
                .filter(cr -> {
                    CourseClassificationView cls = context.getClassification(cr.courseCode());
                    return cls != null
                            && cls.courseType() == CourseType.ACADEMIC_FOUNDATION
                            && "실험".equals(cls.subCategory());
                })
                .count();

        return new RuleResult(rule.ruleName(), count >= config.minCount());
    }

    private record Config(int minCount) {}
}
