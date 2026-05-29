package com.donggree.graduation.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * graduation_rule.rule_config JSON 문자열을 파싱하는 패키지 내부 유틸리티.
 * 각 evaluator의 ruleConfig 파싱에 공통으로 사용한다.
 */
final class RuleConfigParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RuleConfigParser() {}

    static <T> T parse(String json, Class<T> configType) {
        try {
            return MAPPER.readValue(json, configType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("rule_config 파싱 실패: " + json, e);
        }
    }
}
