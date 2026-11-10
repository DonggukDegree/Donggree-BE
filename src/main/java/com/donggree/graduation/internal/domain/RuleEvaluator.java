package com.donggree.graduation.internal.domain;

import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.rule.EvaluationRule;

/**
 * 졸업 규칙 하나를 평가하는 도메인 서비스 인터페이스.
 * supportedTypeName()이 rule_type.type_name과 일치하는 evaluator가 해당 규칙을 처리한다.
 */
public interface RuleEvaluator {

    /** 이 evaluator가 처리하는 rule_type.type_name을 반환한다. */
    String supportedTypeName();

    /**
     * 졸업 규칙을 평가하여 충족 여부와 규칙명을 담은 RuleResult를 반환한다.
     * 저장 형식 해석이 끝난 유형별 설정만 사용한다.
     */
    RuleResult evaluate(EvaluationRule rule, EvaluationContext context);
}
