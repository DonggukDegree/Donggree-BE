package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 영역별 최소 이수 학점 규칙 평가기.
 * 공통교양, 기본소양, MSC, 전공 최소학점 등에 공통으로 사용한다.
 * ruleConfig:
 *   {"courseType": "COMMON_GENERAL", "subCategories": null, "minCredits": 17}
 *   {"courseType": "ACADEMIC_FOUNDATION", "subCategories": ["기본소양"], "minCredits": 6}
 *   {"courseType": "ACADEMIC_FOUNDATION", "subCategories": ["수학", "과학"], "minCredits": 21}
 *
 * subCategories가 null이면 courseType 전체 학점을 합산한다.
 */
@Component
public class MinAreaCreditsEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "MIN_AREA_CREDITS";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        CourseType courseType = CourseType.valueOf(config.courseType());

        int earned = (config.subCategories() == null)
                ? context.getTotalPassedCreditsByType(courseType)
                : context.getTotalPassedCreditsByTypeAndSubCategories(courseType, config.subCategories());

        return new RuleResult(rule.ruleName(), earned >= config.minCredits());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(String courseType, List<String> subCategories, int minCredits) {}
}
