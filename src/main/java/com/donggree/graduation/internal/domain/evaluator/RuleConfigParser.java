package com.donggree.graduation.internal.domain.evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * graduation_rule.rule_config JSON 문자열을 파싱하는 evaluator 내부 유틸리티.
 */
final class RuleConfigParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RuleConfigParser() {}

    static <T> T parse(String json, Class<T> configType) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("rule_config가 비어 있습니다.");
        }
        try {
            return MAPPER.readValue(json, configType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("rule_config 파싱 실패: " + json, e);
        }
    }
}
