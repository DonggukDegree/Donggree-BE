package com.donggree.graduation.internal.domain;

import com.donggree.curriculum.GraduationRuleView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/** 대체 과목 이수 시 MIN_CREDITS 목표 학점을 판정과 리포트에서 동일하게 조정한다. */
public final class CreditAdjustmentCalculator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CreditAdjustmentCalculator() {}

    public static int adjustedTarget(GraduationRuleView rule, EvaluationContext context, int baseTarget) {
        try {
            JsonNode adjustments = MAPPER.readTree(rule.ruleConfig()).path("creditAdjustments");
            int reduction = 0;
            for (JsonNode adjustment : adjustments) {
                List<String> requiredCodes = readCodes(adjustment.path("requiredCourseCodes"));
                List<String> replacementCodes = readCodes(adjustment.path("replacementCourseCodes"));
                int credits = adjustment.path("credits").asInt(0);
                if (requiredCodes.isEmpty() || replacementCodes.isEmpty() || credits <= 0) continue;

                boolean requiredCoursePassed =
                        context.hasPassedAnyCourseByCodeForRule(rule.courseType(), requiredCodes);
                boolean replacementPassed =
                        context.hasPassedAnyCourseByCodeForRule(rule.courseType(), replacementCodes);
                if (!requiredCoursePassed && replacementPassed) reduction += credits;
            }
            return Math.max(0, baseTarget - reduction);
        } catch (Exception ignored) {
            return baseTarget;
        }
    }

    private static List<String> readCodes(JsonNode node) {
        List<String> codes = new ArrayList<>();
        if (!node.isArray()) return codes;
        for (JsonNode value : node) {
            if (value.isTextual() && !value.asText().isBlank()) codes.add(value.asText());
        }
        return codes;
    }
}
