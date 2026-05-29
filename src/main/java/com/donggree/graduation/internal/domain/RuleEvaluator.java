package com.donggree.graduation.internal.domain;

import com.donggree.curriculum.GraduationRuleView;

/**
 * 졸업 규칙 하나를 평가하는 도메인 서비스 인터페이스.
 * supportedTypeName()이 rule_type.type_name과 일치하는 evaluator가 해당 규칙을 처리한다.
 */
public interface RuleEvaluator {

    /** 이 evaluator가 처리하는 rule_type.type_name을 반환한다. */
    String supportedTypeName();

    /**
     * 졸업 규칙을 평가하여 충족 여부와 규칙명을 담은 RuleResult를 반환한다.
     * ruleConfig JSON 파싱 실패 시 IllegalStateException을 던진다.
     */
    RuleResult evaluate(GraduationRuleView rule, EvaluationContext context);
}
