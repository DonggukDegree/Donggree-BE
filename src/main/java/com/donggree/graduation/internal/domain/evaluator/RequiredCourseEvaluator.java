package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;

import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import org.springframework.stereotype.Component;

/**
 * 필수 과목 이수 규칙 평가기. 수학필수·전공필수 등에 공통으로 사용한다.
 * ruleConfig: {"courseName": "기초프로그래밍"}
 * 해당 과목명을 이수한 course_record가 있으면 충족으로 판정한다.
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
        boolean satisfied = context.hasPassedCourseByName(config.courseName());
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private record Config(String courseName) {}
}
