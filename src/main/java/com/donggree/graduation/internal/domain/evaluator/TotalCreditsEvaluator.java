package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;

import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import org.springframework.stereotype.Component;

/**
 * 총 취득학점 규칙 평가기.
 * ruleConfig: {"minCredits": 130}
 * transcript.totalCredits가 minCredits 이상이면 충족으로 판정한다.
 */
@Component
public class TotalCreditsEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "TOTAL_CREDITS";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        boolean satisfied = context.getTranscript().totalCredits() >= config.minCredits();
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private record Config(int minCredits) {}
}
