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
    private static final Set<String> GPA_SCOPES = Set.of("TOTAL", "MAJOR");

    private GraduationRuleConfigValidator() {}

    /** 필수과목의 기존 OR 목록 또는 세트 OR / 그룹 AND / 코드 OR 중 한 방식만 허용한다. */
    public static boolean hasValidRequiredCourses(String typeName, String ruleConfig) {
        if (!"REQUIRED_COURSE".equals(typeName)) return true;
        try {
            JsonNode config = MAPPER.readTree(ruleConfig);
            if (!config.hasNonNull("requiredCourseSets")) {
                return hasValidCodes(config.path("courseCodes"));
            }
            if (config.hasNonNull("courseCodes")) return false;
            JsonNode sets = config.path("requiredCourseSets");
            if (!sets.isArray() || sets.isEmpty()) return false;
            for (JsonNode set : sets) {
                if (!set.isArray() || set.isEmpty()) return false;
                for (JsonNode group : set) {
                    if (!hasValidCodes(group)) return false;
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasValidCodes(JsonNode codes) {
        if (!codes.isArray() || codes.isEmpty()) return false;
        for (JsonNode code : codes) {
            if (!code.isTextual() || code.asText().isBlank()) return false;
        }
        return true;
    }

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

    public static boolean hasValidGpaScope(String typeName, String ruleConfig) {
        if (!"GPA".equals(typeName)) return true;
        try {
            JsonNode scope = MAPPER.readTree(ruleConfig).path("gpaScope");
            return scope.isMissingNode() || scope.isNull() || GPA_SCOPES.contains(scope.asText());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasValidTeachingAptitudeCount(String typeName, String ruleConfig) {
        if (!"TEACHING_APTITUDE".equals(typeName)) return true;
        try {
            return MAPPER.readTree(ruleConfig).path("minCount").canConvertToInt()
                    && MAPPER.readTree(ruleConfig).path("minCount").asInt() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasValidCreditAdjustments(String typeName, String ruleConfig) {
        if (!"MIN_CREDITS".equals(typeName)) return true;
        try {
            JsonNode adjustments = MAPPER.readTree(ruleConfig).path("creditAdjustments");
            if (adjustments.isMissingNode() || adjustments.isNull()) return true;
            if (!adjustments.isArray()) return false;
            for (JsonNode adjustment : adjustments) {
                if (!hasValidCodes(adjustment.path("requiredCourseCodes"))
                        || !hasValidCodes(adjustment.path("replacementCourseCodes"))
                        || !adjustment.path("credits").canConvertToInt()
                        || adjustment.path("credits").asInt() <= 0) return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
