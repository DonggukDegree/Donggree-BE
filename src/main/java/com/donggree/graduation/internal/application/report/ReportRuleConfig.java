package com.donggree.graduation.internal.application.report;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.global.logging.RequestDiagnostics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/** 리포트 표시용 설정 해석. 판정기의 엄격한 검증과 별도로 기존 기본값을 유지한다. */
final class ReportRuleConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportRuleConfig() {}

    static List<String> extractRequiredCodes(List<GraduationRuleView> rules) {
        List<String> codes = new ArrayList<>();
        for (GraduationRuleView rule : rules) {
            if (!"REQUIRED_COURSE".equals(rule.typeName())) continue;
            codes.addAll(requiredCodes(rule));
        }
        return codes;
    }

    static List<String> requiredCodes(GraduationRuleView rule) {
        try {
            JsonNode config = MAPPER.readTree(rule.ruleConfig());
            if (!config.hasNonNull("requiredCourseSets")) {
                return parseStringList(rule.ruleConfig(), "courseCodes");
            }
            List<String> codes = new ArrayList<>();
            for (JsonNode set : config.path("requiredCourseSets")) {
                for (JsonNode group : set) {
                    for (JsonNode code : group) {
                        if (code.isTextual()) codes.add(code.asText());
                    }
                }
            }
            return codes.stream().distinct().toList();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            RequestDiagnostics.fallback("report_required_courses", e);
            return List.of();
        }
    }

    static List<GraduationRuleView> applicableRequiredRules(
            List<GraduationRuleView> rules, String studentEnglishLevel) {
        List<GraduationRuleView> result = new ArrayList<>();
        for (GraduationRuleView rule : rules) {
            if (!"REQUIRED_COURSE".equals(rule.typeName())) continue;
            List<String> exemptLevels = parseStringList(rule.ruleConfig(), "exemptEnglishLevels");
            if (!exemptLevels.isEmpty() && studentEnglishLevel != null && exemptLevels.contains(studentEnglishLevel)) {
                continue;
            }
            List<String> requiredLevels = parseStringList(rule.ruleConfig(), "requiredEnglishLevels");
            if (!requiredLevels.isEmpty()
                    && (studentEnglishLevel == null || !requiredLevels.contains(studentEnglishLevel))) {
                continue;
            }
            result.add(rule);
        }
        return result;
    }

    static List<String> parseStringList(String json, String field) {
        if (json == null) return List.of();
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode arr = node.path(field);
            if (arr.isNull() || arr.isMissingNode() || !arr.isArray()) return List.of();
            List<String> result = new ArrayList<>();
            arr.forEach(n -> result.add(n.asText()));
            return result;
        } catch (JsonProcessingException e) {
            RequestDiagnostics.fallback("report_rule_list", e);
            return List.of();
        }
    }

    static boolean hasNumberField(String ruleConfig, String fieldName) {
        if (ruleConfig == null) return false;
        try {
            return MAPPER.readTree(ruleConfig).path(fieldName).isNumber();
        } catch (JsonProcessingException e) {
            RequestDiagnostics.fallback("report_rule_number", e);
            return false;
        }
    }

    static int parseIntField(String ruleConfig, String fieldName, int defaultValue) {
        if (ruleConfig == null) return defaultValue;
        try {
            JsonNode node = MAPPER.readTree(ruleConfig);
            JsonNode field = node.path(fieldName);
            return field.isMissingNode() ? defaultValue : field.asInt(defaultValue);
        } catch (JsonProcessingException e) {
            RequestDiagnostics.fallback("report_rule_integer", e);
            return defaultValue;
        }
    }
}
