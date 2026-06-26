package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 총 평점평균 규칙 평가기.
 * ruleConfig: {"minGpa": 2.0}
 * transcript.gpa가 minGpa 이상이면 충족으로 판정한다.
 */
@Component
public class GpaEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "GPA";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        BigDecimal minGpa = BigDecimal.valueOf(config.minGpa());
        BigDecimal gpa = context.getTranscript().gpa();
        BigDecimal currentGpa = gpa != null ? gpa : BigDecimal.ZERO;
        boolean satisfied = currentGpa.compareTo(minGpa) >= 0;
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private record Config(double minGpa) {}
}
