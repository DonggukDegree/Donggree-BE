package com.donggree.graduation.internal.infrastructure.rule;

import com.donggree.graduation.internal.application.evaluation.RuleConfigurationReader;
import com.donggree.graduation.internal.domain.rule.CreditAdjustment;
import com.donggree.graduation.internal.domain.rule.RuleConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 유형별 JSON 호환 정책을 가진 판정 설정 어댑터. */
@Component
public class JacksonRuleConfigurationReader implements RuleConfigurationReader {
    private static final ObjectMapper MAPPER = RuleJson.MAPPER.copy();
    private static final Map<String, Class<? extends RuleConfiguration>> TYPES = Map.of(
            "TOTAL_CREDITS", RuleConfiguration.TotalCredits.class,
            "GPA", RuleConfiguration.Gpa.class,
            "MIN_CREDITS", RuleConfiguration.MinCredits.class,
            "PREREQUISITE", RuleConfiguration.Prerequisite.class,
            "REQUIRED_COURSE", RuleConfiguration.RequiredCourse.class,
            "ENGLISH_COURSE", RuleConfiguration.EnglishCourse.class,
            "THESIS", RuleConfiguration.Thesis.class,
            "TEACHING_APTITUDE", RuleConfiguration.TeachingAptitude.class);

    static {
        for (Class<?> type : List.of(
                RuleConfiguration.MinCredits.class,
                RuleConfiguration.Prerequisite.class,
                RuleConfiguration.RequiredCourse.class,
                RuleConfiguration.EnglishCourse.class,
                RuleConfiguration.Thesis.class)) {
            MAPPER.addMixIn(type, IgnoreUnknown.class);
        }
    }

    @Override
    public RuleConfiguration read(String typeName, String settings) {
        Class<? extends RuleConfiguration> type = TYPES.get(typeName);
        // SCIENCE_CONFLICT에는 설정이 없으며, 기존 미지원 유형도 이 단계에서 새로 거부하지 않는다.
        if (type == null) return new RuleConfiguration.NoSettings();
        requireSettings(settings);
        try {
            return MAPPER.readValue(settings, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("rule_config 파싱 실패", ex);
        }
    }

    @Override
    public List<String> readMajorRoles(String settings) {
        requireSettings(settings);
        try {
            JsonNode roles = MAPPER.readTree(settings).path("applicableMajorRoles");
            if (!roles.isArray()) return List.of();
            return MAPPER.convertValue(
                    roles, MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("rule_config 파싱 실패", ex);
        }
    }

    @Override
    public List<CreditAdjustment> readCreditAdjustments(String settings) {
        try {
            List<CreditAdjustment> result = new ArrayList<>();
            for (JsonNode row : MAPPER.readTree(settings).path("creditAdjustments")) {
                List<String> required = codes(row.path("requiredCourseCodes"));
                List<String> replacement = codes(row.path("replacementCourseCodes"));
                int credits = row.path("credits").asInt(0);
                if (!required.isEmpty() && !replacement.isEmpty() && credits > 0) {
                    result.add(new CreditAdjustment(required, replacement, credits));
                }
            }
            return List.copyOf(result);
        } catch (Exception ignored) {
            // 대체과목 설정을 읽을 수 없으면 감면하지 않던 기존 정책 유지.
            return List.of();
        }
    }

    private static List<String> codes(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (!node.isArray()) return result;
        for (JsonNode value : node) {
            if (value.isTextual() && !value.asText().isBlank()) result.add(value.asText());
        }
        return result;
    }

    private static void requireSettings(String settings) {
        if (settings == null || settings.isBlank()) throw new IllegalStateException("rule_config가 비어 있습니다.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class IgnoreUnknown {}
}
