package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 필수 과목 이수 규칙 평가기. 수학필수·전공필수 등에 공통으로 사용한다.
 * ruleConfig: {"courseCodes": ["PRI4001"]}
 * 단일 코드면 1개짜리 배열, 동일유사 교과목이면 여러 코드 배열 — 하나라도 이수하면 충족.
 */
@Component
public class RequiredCourseEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "REQUIRED_COURSE";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        boolean satisfied = context.hasPassedAnyCourseByCode(config.courseCodes());
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private record Config(List<String> courseCodes) {}
}
