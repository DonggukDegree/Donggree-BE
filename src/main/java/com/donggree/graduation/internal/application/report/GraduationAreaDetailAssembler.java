package com.donggree.graduation.internal.application.report;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluation.CourseClassification;
import com.donggree.graduation.internal.domain.evaluation.EvaluationContext;
import com.donggree.graduation.internal.domain.evaluation.EvaluationCourse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 영역별 과목·소분류와 이수 상태 표시 조립. */
final class GraduationAreaDetailAssembler {
    private static final String MIN_CREDITS = "MIN_CREDITS";

    private final ReportRuleConfig config;
    private final AcademicFoundationCreditTargets foundation;
    private final ReportCreditTargets creditTargets;

    GraduationAreaDetailAssembler(
            ReportRuleConfig config, AcademicFoundationCreditTargets foundation, ReportCreditTargets creditTargets) {
        this.config = config;
        this.foundation = foundation;
        this.creditTargets = creditTargets;
    }

    List<AreaDetailReadModel.AreaSection> buildAreaSections(
            CourseType courseType,
            List<GraduationRuleView> areaRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            Map<String, CourseClassification> allCls,
            List<String> roleRequiredCodes) {

        String fallbackArea = ReportCreditTargets.courseTypeKoreanName(courseType);
        List<EvaluationCourse> allPassed = context.getPassedCoursesByType(courseType);
        String studentEnglishLevel = context.getTranscript().englishLevel();

        // 이 이수구분에 속한 필수 규칙 — 미충족 시 규칙명·0학점 placeholder로 노출하기 위해 사용한다.
        record RequiredRule(GraduationRuleView rule, List<String> codes, String areaName) {}
        List<RequiredRule> requiredRules = new ArrayList<>();
        for (GraduationRuleView rule : config.applicableRequiredRules(areaRules, studentEnglishLevel)) {
            List<String> codes = config.requiredCodes(rule);
            // course_classification을 우선하되 PDF 원문과 동일한 표시명 정규화 적용
            String ruleArea = codes.stream()
                    .map(allCls::get)
                    .filter(Objects::nonNull)
                    .map(CourseClassification::areaName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            // 미등록 과목이지만 사용자가 이수했다면 pdfAreaName에서 area를 추론
            if (ruleArea == null) {
                ruleArea = allPassed.stream()
                        .filter(cr -> context.codeMatchesAny(cr.courseCode(), codes))
                        .findFirst()
                        .map(EvaluationCourse::pdfAreaName)
                        .orElse(null);
            }
            requiredRules.add(new RequiredRule(rule, codes, resolveAreaName(ruleArea, courseType, null)));
        }

        // 단일 영역 목표학점만 섹션에 귀속시킨다.
        // 여러 영역을 합산하는 규칙은 어느 한 영역의 목표로 볼 수 없으므로 섹션 targetCredits는 0으로 두고,
        // 미충족 시 unsatisfiedReasons에 규칙명으로 노출된다.
        Map<String, Integer> targetCreditsByArea;
        if (courseType == CourseType.ACADEMIC_FOUNDATION) {
            // 영역명뿐 아니라 학수번호·소분류 등 실제 대상 범위의 포함 관계까지 반영한다.
            targetCreditsByArea = foundation.byArea(areaRules, context);
        } else {
            targetCreditsByArea = new HashMap<>();
            for (GraduationRuleView rule : areaRules) {
                if (!creditTargets.isCreditTargetRule(rule)) continue;
                List<String> areaNames = config.parseStringList(rule.ruleConfig(), "areaNames");
                if (areaNames.size() == 1) {
                    targetCreditsByArea.merge(
                            resolveAreaName(areaNames.get(0), courseType, fallbackArea),
                            config.adjustedTarget(
                                    rule, context, config.parseIntField(rule.ruleConfig(), "minCredits", 0)),
                            Math::max);
                }
            }
        }

        LinkedHashMap<String, List<EvaluationCourse>> coursesByArea = new LinkedHashMap<>();
        for (EvaluationCourse cr : allPassed) {
            CourseClassification cls = cr.courseCode() != null ? allCls.get(cr.courseCode()) : null;
            String area;
            if (cls != null && cls.areaName() != null) {
                area = resolveAreaName(cls.areaName(), courseType, fallbackArea);
            } else {
                area = resolveAreaName(cr.pdfAreaName(), courseType, fallbackArea);
            }
            coursesByArea.computeIfAbsent(area, k -> new ArrayList<>()).add(cr);
        }
        // 미충족 필수과목 영역도 섹션에 포함 (충족된 필수는 학생이 실제 이수한 영역에 자연히 표시됨)
        for (RequiredRule rr : requiredRules) {
            if (isRuleSatisfied(rr.rule(), resultByRuleId)) continue;
            String area = rr.areaName() != null ? rr.areaName() : fallbackArea;
            coursesByArea.computeIfAbsent(area, k -> new ArrayList<>());
        }

        List<AreaDetailReadModel.AreaSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<EvaluationCourse>> entry : coursesByArea.entrySet()) {
            String areaName = entry.getKey();
            List<EvaluationCourse> areaCourses = entry.getValue();
            List<AreaDetailReadModel.CourseItem> items = new ArrayList<>();

            // 이수 과목 분류 — 필수 학수번호를 충족시킨 과목은 학생 수강 내역대로 SATISFIED로 표시한다.
            // subCategory(실험, 개론 등)가 있으면 alias로 그룹핑, 없으면 개별 항목.
            List<EvaluationCourse> requiredFulfilled = new ArrayList<>();
            LinkedHashMap<String, List<EvaluationCourse>> bySubCategory = new LinkedHashMap<>();
            List<EvaluationCourse> individualOptional = new ArrayList<>();
            for (EvaluationCourse cr : areaCourses) {
                if (context.codeMatchesAny(cr.courseCode(), roleRequiredCodes)) {
                    requiredFulfilled.add(cr);
                    continue;
                }
                CourseClassification cls = cr.courseCode() != null ? allCls.get(cr.courseCode()) : null;
                String subCat = cls != null ? cls.subCategory() : null;
                if (subCat != null) {
                    bySubCategory
                            .computeIfAbsent(subCat, k -> new ArrayList<>())
                            .add(cr);
                } else {
                    individualOptional.add(cr);
                }
            }

            // 충족된 필수 과목 (학생이 실제 이수한 과목명·학점)
            for (EvaluationCourse cr : requiredFulfilled) {
                items.add(new AreaDetailReadModel.CourseItem(cr.courseName(), cr.credits(), "SATISFIED", null));
            }
            // 미충족 필수 과목 placeholder (규칙명·0학점) — 규칙이 속한 이수구분 탭에만 노출
            for (RequiredRule rr : requiredRules) {
                if (isRuleSatisfied(rr.rule(), resultByRuleId)) continue;
                String ruleArea = rr.areaName() != null ? rr.areaName() : fallbackArea;
                if (!areaName.equals(ruleArea)) continue;
                items.add(new AreaDetailReadModel.CourseItem(
                        extractCourseTitle(rr.rule().ruleName()), 0, "UNSATISFIED", null));
            }
            for (Map.Entry<String, List<EvaluationCourse>> subEntry : bySubCategory.entrySet()) {
                String subCat = subEntry.getKey();
                List<EvaluationCourse> subCourses = subEntry.getValue();
                int subEarned =
                        subCourses.stream().mapToInt(EvaluationCourse::credits).sum();
                List<String> detail =
                        subCourses.stream().map(EvaluationCourse::courseName).toList();
                String subStatus = resolveSubCategoryStatus(subCat, areaRules, resultByRuleId);
                items.add(new AreaDetailReadModel.CourseItem(subCat, subEarned, subStatus, detail));
            }
            for (EvaluationCourse cr : individualOptional) {
                items.add(new AreaDetailReadModel.CourseItem(cr.courseName(), cr.credits(), "OPTIONAL", null));
            }

            int earnedCredits =
                    areaCourses.stream().mapToInt(EvaluationCourse::credits).sum();
            int targetCredits = targetCreditsByArea.getOrDefault(areaName, 0);

            boolean areaSatisfied = requiredRules.stream()
                    .filter(rr -> areaName.equals(rr.areaName() != null ? rr.areaName() : fallbackArea))
                    .allMatch(rr -> isRuleSatisfied(rr.rule(), resultByRuleId));
            if (targetCredits > 0) {
                areaSatisfied = areaSatisfied && earnedCredits >= targetCredits;
            }
            if (courseType == CourseType.ACADEMIC_FOUNDATION) {
                // 영역 총학점을 채워도 특정 과목·소분류의 최소학점이 부족할 수 있다.
                // 다중 영역 요건은 해당 개별 카드에 귀속하지 않는다.
                areaSatisfied = areaSatisfied
                        && areaRules.stream()
                                .filter(creditTargets::isCreditTargetRule)
                                .filter(rule -> config.parseStringList(rule.ruleConfig(), "areaNames").stream()
                                        .distinct()
                                        .toList()
                                        .equals(List.of(areaName)))
                                .allMatch(rule -> isRuleSatisfied(rule, resultByRuleId));
            }

            sections.add(
                    new AreaDetailReadModel.AreaSection(areaName, earnedCredits, targetCredits, areaSatisfied, items));
        }

        return sections;
    }

    static boolean isRuleSatisfied(GraduationRuleView rule, Map<Long, RuleResult> resultByRuleId) {
        RuleResult result = resultByRuleId.get(rule.id());
        return result != null && result.satisfied();
    }

    static String extractCourseTitle(String ruleName) {
        for (String suffix : new String[] {"은 필수", "는 필수", "을 이수", "를 이수"}) {
            int idx = ruleName.indexOf(suffix);
            if (idx > 0) return ruleName.substring(0, idx);
        }
        return ruleName;
    }

    static String resolveAreaName(String areaName, CourseType courseType, String fallback) {
        if (areaName == null) return fallback;
        if (courseType == CourseType.FIRST_MAJOR || courseType == CourseType.SECOND_MAJOR) {
            return switch (areaName) {
                case "기초" -> "전공기초";
                case "전문" -> "전공전문";
                default -> areaName;
            };
        }
        return areaName;
    }

    String resolveSubCategoryStatus(
            String subCategory, List<GraduationRuleView> areaRules, Map<Long, RuleResult> resultByRuleId) {
        return areaRules.stream()
                .filter(r -> MIN_CREDITS.equals(r.typeName()))
                .filter(r ->
                        config.parseStringList(r.ruleConfig(), "subCategories").contains(subCategory))
                .findFirst()
                .map(r -> isRuleSatisfied(r, resultByRuleId) ? "SATISFIED" : "UNSATISFIED")
                .orElse("OPTIONAL");
    }
}
