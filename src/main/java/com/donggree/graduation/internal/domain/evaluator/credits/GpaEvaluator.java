package com.donggree.graduation.internal.domain.evaluator.credits;

import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;
import com.donggree.graduation.internal.domain.rule.RuleConfiguration;
import java.math.BigDecimal;

/**
 * 총 평점평균 규칙 평가기.
 * ruleConfig: {"minGpa": 2.0, "gpaScope": "TOTAL" | "MAJOR" | "TEACHING"}
 * gpaScope가 없으면 기존 규칙과 같이 총 평점으로 판정한다.
 */
public class GpaEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "GPA";
    }

    @Override
    public RuleResult evaluate(EvaluationRule rule, EvaluationContext context) {
        RuleConfiguration.Gpa config = rule.configuration(RuleConfiguration.Gpa.class);
        BigDecimal minGpa = BigDecimal.valueOf(config.minGpa());
        BigDecimal gpa =
                switch (config.gpaScope() == null ? "TOTAL" : config.gpaScope()) {
                    case "MAJOR" -> context.getTranscript().majorGpa();
                    case "TEACHING" -> context.getTranscript().teachingGpa();
                    default -> context.getTranscript().gpa();
                };
        BigDecimal currentGpa = gpa != null ? gpa : BigDecimal.ZERO;
        boolean satisfied = currentGpa.compareTo(minGpa) >= 0;
        return new RuleResult(rule.ruleName(), satisfied);
    }
}
