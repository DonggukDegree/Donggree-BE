package com.donggree.graduation.internal.domain;

import java.util.List;

/** MIN_CREDITS·REQUIRED_COURSE·THESIS·ENGLISH_COURSE의 전공 역할 적용 조건을 판별한다. */
public final class MajorRoleRuleMatcher {

    private static final List<String> ROLE_AWARE_TYPES =
            List.of("MIN_CREDITS", "REQUIRED_COURSE", "THESIS", "ENGLISH_COURSE");

    private MajorRoleRuleMatcher() {}

    public static boolean supportsMajorRole(String typeName) {
        return ROLE_AWARE_TYPES.contains(typeName);
    }

    public static boolean applies(String typeName, List<String> configuredRoles, MajorRole role) {
        if (!supportsMajorRole(typeName)) {
            return role.isPrimary();
        }

        if (configuredRoles.isEmpty()) {
            // 기존 논문·영어강의는 두 주전공 역할 모두에 적용됐다. 옵션 도입으로 요건이 빠지지 않게 유지한다.
            if ("THESIS".equals(typeName) || "ENGLISH_COURSE".equals(typeName)) return role.isPrimary();
            // 적용 대상은 필수값이다. 단, 마이그레이션 전 기존 규칙은 단일전공으로 안전하게 해석한다.
            return role == MajorRole.SINGLE_PRIMARY;
        }
        return configuredRoles.contains(role.name());
    }
}
