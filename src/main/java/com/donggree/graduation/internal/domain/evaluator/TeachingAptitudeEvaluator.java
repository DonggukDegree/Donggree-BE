package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import org.springframework.stereotype.Component;

/** PDF에 기록된 교직인적성 합격 횟수의 최소 기준을 판정한다. */
@Component
public class TeachingAptitudeEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "TEACHING_APTITUDE";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        Integer passedCount = context.getTranscript().teachingAptitudeCount();
        return new RuleResult(rule.ruleName(), passedCount != null && passedCount >= config.minCount());
    }

    private record Config(int minCount) {}
}
