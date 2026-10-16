package com.donggree.graduation.internal.domain.evaluator.qualification;

import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import com.donggree.graduation.internal.domain.rule.RuleConfiguration;

/** PDF에 기록된 교직인적성 합격 횟수의 최소 기준을 판정한다. */
public class TeachingAptitudeEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "TEACHING_APTITUDE";
    }

    @Override
    public RuleResult evaluate(EvaluationRule rule, EvaluationContext context) {
        RuleConfiguration.TeachingAptitude config = rule.configuration(RuleConfiguration.TeachingAptitude.class);
        Integer passedCount = context.getTranscript().teachingAptitudeCount();
        return new RuleResult(rule.ruleName(), passedCount != null && passedCount >= config.minCount());
    }
}
