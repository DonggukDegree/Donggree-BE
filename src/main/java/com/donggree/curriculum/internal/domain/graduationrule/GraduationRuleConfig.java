package com.donggree.curriculum.internal.domain.graduationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.TreeMap;
import java.util.TreeSet;

/** 규칙 옵션의 저장·중복 비교 표현. 객체 키와 전공 역할 선택 순서만으로 중복 검사를 우회하지 못하게 한다. */
public final class GraduationRuleConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private GraduationRuleConfig() {}

    public static String normalize(String config) {
        if (config == null || config.isBlank()) {
            throw new IllegalArgumentException("ruleConfig must be a JSON object");
        }
        try {
            JsonNode parsed = MAPPER.readTree(config);
            if (parsed == null || !parsed.isObject()) {
                throw new IllegalArgumentException("ruleConfig must be a JSON object");
            }
            ObjectNode normalized = (ObjectNode) normalizeNode(parsed);
            JsonNode roles = normalized.get("applicableMajorRoles");
            if (roles != null && roles.isArray()) {
                TreeSet<String> selected = new TreeSet<>();
                boolean textual = true;
                for (JsonNode role : roles) {
                    textual &= role.isTextual();
                    selected.add(role.asText());
                }
                // 잘못된 역할 값의 거절은 기존 종류별 검증기가 담당한다.
                if (textual) {
                    ArrayNode orderedRoles = MAPPER.createArrayNode();
                    selected.forEach(orderedRoles::add);
                    normalized.set("applicableMajorRoles", orderedRoles);
                }
            }
            return MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("ruleConfig must be a JSON object", ex);
        }
    }

    private static JsonNode normalizeNode(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.properties().forEach(field -> fields.put(field.getKey(), normalizeNode(field.getValue())));
            ObjectNode result = MAPPER.createObjectNode();
            fields.forEach(result::set);
            return result;
        }
        if (node.isArray()) {
            // 다른 배열은 순서가 의미를 가질 수 있으므로 임의 정렬·중복 제거하지 않는다.
            ArrayNode result = MAPPER.createArrayNode();
            node.forEach(value -> result.add(normalizeNode(value)));
            return result;
        }
        if (node.isNumber()) {
            // PostgreSQL jsonb와 같이 36, 36.0, 3.6e1을 같은 값으로 비교한다.
            BigDecimal value = node.decimalValue().stripTrailingZeros();
            return value.scale() <= 0 ? BigIntegerNode.valueOf(value.toBigIntegerExact()) : DecimalNode.valueOf(value);
        }
        return node;
    }
}
