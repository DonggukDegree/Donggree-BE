package com.donggree.graduation.internal.domain.evaluator.credits;

import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import com.donggree.graduation.internal.domain.rule.RuleConfiguration;

/**
 * 총 취득학점 규칙 평가기.
 * ruleConfig: {"minCredits": 130}
 * transcript.totalCredits가 minCredits 이상이면 충족으로 판정한다.
 */
public class TotalCreditsEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "TOTAL_CREDITS";
    }

    @Override
    public RuleResult evaluate(EvaluationRule rule, EvaluationContext context) {
        RuleConfiguration.TotalCredits config = rule.configuration(RuleConfiguration.TotalCredits.class);
        boolean satisfied = context.getTranscript().totalCredits() >= config.minCredits();
        return new RuleResult(rule.ruleName(), satisfied);
    }
}
