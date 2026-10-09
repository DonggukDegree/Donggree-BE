package com.donggree.graduation.internal.application;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.graduation.internal.domain.CreditAdjustmentCalculator;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 학문기초 리포트의 표시용 목표학점 계산. 실제 규칙 판정·수강 학점 집계에는 사용하지 않는다. */
final class AcademicFoundationCreditTargets {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> SELECTORS =
            List.of("courseType", "areaNames", "subCategories", "pdfAreaNames", "pdfCourseTypeNames", "courseCodes");
    private static final int AREA_NAMES = 1;
    private static final int COURSE_CODES = 5;

    private AcademicFoundationCreditTargets() {}

    static int total(List<GraduationRuleView> rules, EvaluationContext context) {
        return rollUp(readTargets(rules, context));
    }

    /** 단일 영역에 직접 지정된 목표만 카드에 귀속한다. 다중 영역·전체 목표는 임의 배분하지 않는다. */
    static Map<String, Integer> byArea(List<GraduationRuleView> rules, EvaluationContext context) {
        Map<String, Map<Scope, Integer>> targetsByArea = new LinkedHashMap<>();
        readTargets(rules, context).forEach((scope, credits) -> {
            List<String> areas = scope.selectors().get(AREA_NAMES);
            if (areas.size() == 1) {
                targetsByArea
                        .computeIfAbsent(areas.getFirst(), ignored -> new LinkedHashMap<>())
                        .put(scope, credits);
            }
        });
        Map<String, Integer> result = new LinkedHashMap<>();
        targetsByArea.forEach((area, targets) -> result.put(area, rollUp(targets)));
        return result;
    }

    private static Map<Scope, Integer> readTargets(List<GraduationRuleView> rules, EvaluationContext context) {
        Map<Scope, Integer> targets = new LinkedHashMap<>();
        for (GraduationRuleView rule : rules) {
            if (!"MIN_CREDITS".equals(rule.typeName()) || rule.ruleConfig() == null) continue;
            try {
                JsonNode config = MAPPER.readTree(rule.ruleConfig());
                if (config == null || !config.path("minCredits").isNumber()) continue;
                int credits = CreditAdjustmentCalculator.adjustedTarget(
                        rule, context, config.path("minCredits").asInt());
                if (credits <= 0) continue;
                List<List<String>> selectors = new ArrayList<>();
                for (String field : SELECTORS) {
                    List<String> values = values(config.path(field));
                    // DAI*와 DAI1001을 함께 쓴 경우에도 DAI* 하나와 동일한 범위로 비교한다.
                    if ("courseCodes".equals(field)) {
                        List<String> patterns = values;
                        values = patterns.stream()
                                .filter(value -> patterns.stream()
                                        .noneMatch(other -> !other.equals(value) && coversCode(other, value)))
                                .toList();
                    }
                    selectors.add(values);
                }
                targets.merge(new Scope(List.copyOf(selectors)), credits, Math::max);
            } catch (JsonProcessingException ex) {
                RequestDiagnostics.fallback("academic_foundation_target", ex);
                // 기존 표시용 파서와 동일하게 해석 불가능한 설정은 목표 계산에서 제외한다.
                // 실제 설정 오류 검증은 규칙 판정기가 수행한다.
            }
        }
        return targets;
    }

    private static List<String> values(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return List.of();
        if (node.isTextual()) return List.of(node.asText());
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values.stream().distinct().sorted().toList();
    }

    private static int rollUp(Map<Scope, Integer> targets) {
        return rollUp(targets, new HashMap<>());
    }

    private static int rollUp(Map<Scope, Integer> targets, Map<Scope, Integer> resolved) {
        int total = 0;
        for (var entry : targets.entrySet()) {
            Scope scope = entry.getKey();
            // 더 큰 범위에 포함된 요건은 그 부모 안에서 계산하므로 최상위에서 다시 더하지 않는다.
            if (targets.keySet().stream().anyMatch(other -> !other.equals(scope) && other.contains(scope))) continue;
            Integer credits = resolved.get(scope);
            if (credits == null) {
                Map<Scope, Integer> children = new LinkedHashMap<>();
                targets.forEach((other, value) -> {
                    if (!other.equals(scope) && scope.contains(other)) children.put(other, value);
                });
                // 수학9 + 과학9는 수학·과학12 안에 있어도 18학점이 필요하다.
                credits = Math.max(entry.getValue(), rollUp(children, resolved));
                resolved.put(scope, credits);
            }
            // 포함 관계가 아닌 범위는 별도 합산한다. 부분 교차·서로 다른 조건의 의미를 임의로 합치지 않는다.
            total += credits;
        }
        return total;
    }

    /** 비어 있는 선택자는 제한 없음. 모든 선택자가 상대 범위를 포함해야 전체 범위도 포함한다. */
    private record Scope(List<List<String>> selectors) {
        boolean contains(Scope other) {
            for (int i = 0; i < selectors.size(); i++) {
                List<String> outer = selectors.get(i);
                List<String> inner = other.selectors.get(i);
                if (outer.isEmpty()) continue;
                if (inner.isEmpty()) return false;
                if (i == COURSE_CODES) {
                    if (!inner.stream().allMatch(code -> outer.stream().anyMatch(pattern -> coversCode(pattern, code))))
                        return false;
                } else if (!outer.containsAll(inner)) return false;
            }
            return true;
        }
    }

    private static boolean coversCode(String outer, String inner) {
        if (outer.equals(inner)) return true;
        if (!outer.endsWith("*")) return false;
        return inner.startsWith(outer.substring(0, outer.length() - 1));
    }
}
