package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 총 평점평균 규칙 평가기.
 * ruleConfig: {"minGpa": 2.0, "gpaScope": "TOTAL" | "MAJOR"}
 * gpaScope가 없으면 기존 규칙과 같이 총 평점으로 판정한다.
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
        BigDecimal gpa = "MAJOR".equals(config.gpaScope())
                ? context.getTranscript().majorGpa()
                : context.getTranscript().gpa();
        BigDecimal currentGpa = gpa != null ? gpa : BigDecimal.ZERO;
        boolean satisfied = currentGpa.compareTo(minGpa) >= 0;
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private record Config(double minGpa, String gpaScope) {}
}
