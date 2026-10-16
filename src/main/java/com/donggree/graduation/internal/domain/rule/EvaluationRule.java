package com.donggree.graduation.internal.domain.rule;

import com.donggree.curriculum.CourseType;
import java.util.List;

/** 외부 저장·전송 형식에서 변환된 판정용 규칙. */
public record EvaluationRule(
        Long id,
        String typeName,
        CourseType courseType,
        String ruleName,
        RuleConfiguration configuration,
        List<CreditAdjustment> creditAdjustments) {
    public EvaluationRule {
        creditAdjustments = List.copyOf(creditAdjustments);
    }

    public <T extends RuleConfiguration> T configuration(Class<T> type) {
        return type.cast(configuration);
    }
}
