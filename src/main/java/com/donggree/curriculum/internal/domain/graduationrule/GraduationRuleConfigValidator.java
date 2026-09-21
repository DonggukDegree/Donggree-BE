package com.donggree.curriculum.internal.domain.graduationrule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;

/** 규칙 종류별 rule_config의 필수 불변식을 검증한다. */
public final class GraduationRuleConfigValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ROLE_AWARE_TYPES =
            Set.of("MIN_CREDITS", "REQUIRED_COURSE", "THESIS", "ENGLISH_COURSE");
    private static final Set<String> MAJOR_ROLES = Set.of("SINGLE_PRIMARY", "DUAL_PRIMARY", "SECONDARY");

    private GraduationRuleConfigValidator() {}

    public static boolean hasValidMajorRoles(String typeName, String ruleConfig) {
        if (!ROLE_AWARE_TYPES.contains(typeName)) return true;
        try {
            JsonNode roles = MAPPER.readTree(ruleConfig).path("applicableMajorRoles");
            if (!roles.isArray() || roles.isEmpty()) return false;
            for (JsonNode role : roles) {
                if (!role.isTextual() || !MAJOR_ROLES.contains(role.asText())) return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
