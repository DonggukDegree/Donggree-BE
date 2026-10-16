package com.donggree.graduation.internal.application.report;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.evaluation.RuleConfigurationReader;
import com.donggree.graduation.internal.domain.CreditAdjustmentCalculator;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** 표시 설정 조합. 저장 형식 해석은 출력 포트에 위임한다. */
@RequiredArgsConstructor
final class ReportRuleConfig {
    private final ReportSettingsReader reader;
    private final RuleConfigurationReader configurationReader;

    List<String> extractRequiredCodes(List<GraduationRuleView> rules) {
        return rules.stream()
                .filter(r -> "REQUIRED_COURSE".equals(r.typeName()))
                .flatMap(r -> requiredCodes(r).stream())
                .toList();
    }

    List<String> requiredCodes(GraduationRuleView rule) {
        return reader.requiredCodes(rule.ruleConfig());
    }

    List<GraduationRuleView> applicableRequiredRules(List<GraduationRuleView> rules, String studentEnglishLevel) {
        List<GraduationRuleView> result = new ArrayList<>();
        for (GraduationRuleView rule : rules) {
            if (!"REQUIRED_COURSE".equals(rule.typeName())) continue;
            List<String> exemptLevels = parseStringList(rule.ruleConfig(), "exemptEnglishLevels");
            if (!exemptLevels.isEmpty() && studentEnglishLevel != null && exemptLevels.contains(studentEnglishLevel))
                continue;
            List<String> requiredLevels = parseStringList(rule.ruleConfig(), "requiredEnglishLevels");
            if (!requiredLevels.isEmpty()
                    && (studentEnglishLevel == null || !requiredLevels.contains(studentEnglishLevel))) continue;
            result.add(rule);
        }
        return result;
    }

    List<String> parseStringList(String json, String field) {
        return reader.stringList(json, field);
    }

    boolean hasNumberField(String json, String field) {
        return reader.hasNumber(json, field);
    }

    int parseIntField(String json, String field, int defaultValue) {
        return reader.intValue(json, field, defaultValue);
    }

    int adjustedTarget(GraduationRuleView rule, EvaluationContext context, int baseTarget) {
        return CreditAdjustmentCalculator.adjustedTarget(
                rule.courseType(), configurationReader.readCreditAdjustments(rule.ruleConfig()), context, baseTarget);
    }
}
