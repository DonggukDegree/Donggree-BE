package com.donggree.graduation.internal.domain;

import com.donggree.curriculum.GraduationRuleView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** MIN_CREDITS·REQUIRED_COURSE·THESIS·ENGLISH_COURSE의 전공 역할 적용 조건을 판별한다. */
public final class MajorRoleRuleMatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> ROLE_AWARE_TYPES =
            List.of("MIN_CREDITS", "REQUIRED_COURSE", "THESIS", "ENGLISH_COURSE");

    private MajorRoleRuleMatcher() {}

    public static boolean supportsMajorRole(GraduationRuleView rule) {
        return ROLE_AWARE_TYPES.contains(rule.typeName());
    }

    public static boolean applies(GraduationRuleView rule, MajorRole role) {
        if (!supportsMajorRole(rule)) {
            return role.isPrimary();
        }

        List<String> configuredRoles = parseRoles(rule.ruleConfig());
        if (configuredRoles.isEmpty()) {
            // 기존 논문·영어강의는 두 주전공 역할 모두에 적용됐다. 옵션 도입으로 요건이 빠지지 않게 유지한다.
            if ("THESIS".equals(rule.typeName()) || "ENGLISH_COURSE".equals(rule.typeName())) return role.isPrimary();
            // 적용 대상은 필수값이다. 단, 마이그레이션 전 기존 규칙은 단일전공으로 안전하게 해석한다.
            return role == MajorRole.SINGLE_PRIMARY;
        }
        return configuredRoles.contains(role.name());
    }

    private static List<String> parseRoles(String ruleConfig) {
        if (ruleConfig == null || ruleConfig.isBlank()) {
            throw new IllegalStateException("rule_config가 비어 있습니다.");
        }
        try {
            JsonNode roles = MAPPER.readTree(ruleConfig).path("applicableMajorRoles");
            if (!roles.isArray()) return List.of();
            return MAPPER.convertValue(
                    roles, MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("rule_config 파싱 실패: " + ruleConfig, e);
        }
    }
}
