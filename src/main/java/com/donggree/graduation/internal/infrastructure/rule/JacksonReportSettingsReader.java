package com.donggree.graduation.internal.infrastructure.rule;

import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.graduation.internal.application.report.ReportSettingsReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 리포트 표시용 설정 해석. 판정기의 엄격한 검증과 별도로 기존 기본값을 유지한다. */
@Component
public class JacksonReportSettingsReader implements ReportSettingsReader {
    private static final ObjectMapper MAPPER = RuleJson.MAPPER;

    public List<String> requiredCodes(String settings) {
        try {
            JsonNode config = MAPPER.readTree(settings);
            if (!config.hasNonNull("requiredCourseSets")) {
                return stringList(settings, "courseCodes");
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

    public List<String> stringList(String json, String field) {
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

    public boolean hasNumber(String ruleConfig, String fieldName) {
        if (ruleConfig == null) return false;
        try {
            return MAPPER.readTree(ruleConfig).path(fieldName).isNumber();
        } catch (JsonProcessingException e) {
            RequestDiagnostics.fallback("report_rule_number", e);
            return false;
        }
    }

    public int intValue(String ruleConfig, String fieldName, int defaultValue) {
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

    @Override
    public Optional<CreditTarget> creditTarget(String settings, List<String> fields) {
        try {
            JsonNode config = MAPPER.readTree(settings);
            if (config == null || !config.path("minCredits").isNumber()) return Optional.empty();
            List<List<String>> result = new ArrayList<>();
            for (String field : fields) {
                JsonNode node = config.path(field);
                List<String> values = new ArrayList<>();
                if (node.isTextual()) values.add(node.asText());
                else if (node.isArray()) node.forEach(value -> values.add(value.asText()));
                result.add(values.stream().distinct().sorted().toList());
            }
            return Optional.of(new CreditTarget(config.path("minCredits").asInt(), result));
        } catch (JsonProcessingException ex) {
            RequestDiagnostics.fallback("academic_foundation_target", ex);
            return Optional.empty();
        }
    }
}
