package com.donggree.graduation.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection.AreaOverview;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection.Summary;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 판정 결과(RuleResult)와 여러 모듈의 조회 데이터를 읽기 전용 프로젝션으로 조립한다.
 * 외부 모듈 조회(트랜잭션·서비스 호출)는 {@link GraduationQueryService}가 담당하고,
 * 이 컴포넌트는 순수 계산(집계·영역 분류·placeholder 구성)만 수행한다 — 부수효과 없음.
 */
@Component
public class GraduationReportAssembler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 최소 이수량 규칙 타입 — 구 MIN_AREA_CREDITS·SCIENCE_EXPERIMENT를 흡수한 통합 타입. */
    private static final String MIN_CREDITS = "MIN_CREDITS";

    /** 학업 리포트 요약 + courseType별 개요를 조립한다. */
    public GraduationReportProjection assembleReport(
            TranscriptView transcript,
            List<GraduationRuleView> rules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            boolean hasUnsupportedMajor) {
        return new GraduationReportProjection(
                buildSummary(transcript, rules, resultByRuleId),
                buildAreaOverviews(rules, resultByRuleId, context),
                hasUnsupportedMajor,
                transcript.englishPassResult());
    }

    /** 영역별 상세 이수 현황을 조립한다. areaRules는 대상 courseType에 속한 규칙만 넘긴다. */
    public AreaDetailProjection assembleAreaDetail(
            CourseType courseType,
            List<GraduationRuleView> areaRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            Map<String, CourseClassificationView> allCls,
            List<String> roleRequiredCodes) {

        List<AreaDetailProjection.AreaSection> areaDetails =
                buildAreaSections(courseType, areaRules, resultByRuleId, context, allCls, roleRequiredCodes);

        List<String> unsatisfiedReasons = areaRules.stream()
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && !result.satisfied();
                })
                .map(GraduationRuleView::ruleName)
                .distinct()
                .toList();

        AreaDetailProjection.CreditStatus creditStatus = buildTypeCredits(courseType, areaRules, context);

        return new AreaDetailProjection(areaDetails, unsatisfiedReasons, creditStatus);
    }

    /** REQUIRED_COURSE 규칙들의 courseCodes를 모은다 (미이수 필수 과목 영역 조회용). */
    public List<String> requiredCourseCodes(List<GraduationRuleView> rules) {
        return extractRequiredCodes(rules);
    }

    /** 학생에게 실제로 적용되는 REQUIRED_COURSE 규칙을 주전공·복수전공 표시 범위에 맞게 모은다. */
    public List<String> applicableRequiredCourseCodes(
            List<GraduationRuleView> rules, String studentEnglishLevel, CourseType displayedCourseType) {
        boolean secondaryArea = displayedCourseType == CourseType.SECOND_MAJOR;
        return applicableRequiredRules(rules, studentEnglishLevel).stream()
                // 복수전공 규칙은 GraduationQueryService에서 음수 스코프 ID를 부여한다.
                // 역할이 다른 같은 학수번호가 상세 화면에서 잘못 충족 표시되는 것을 막는다.
                .filter(rule -> secondaryArea == (rule.id() < 0))
                .flatMap(r -> parseStringList(r.ruleConfig(), "courseCodes").stream())
                .distinct()
                .toList();
    }

    private Summary buildSummary(
            TranscriptView transcript, List<GraduationRuleView> rules, Map<Long, RuleResult> resultByRuleId) {
        long satisfiedCount =
                resultByRuleId.values().stream().filter(RuleResult::satisfied).count();
        int achievementRate = rules.isEmpty() ? 0 : (int) (satisfiedCount * 100 / rules.size());

        int targetCredits = rules.stream()
                .filter(r -> "TOTAL_CREDITS".equals(r.typeName()))
                .findFirst()
                .map(r -> parseIntField(r.ruleConfig(), "minCredits", 130))
                .orElse(130);

        int earnedCredits = transcript.totalCredits();
        int remainingCredits = Math.max(0, targetCredits - earnedCredits);

        boolean graduated =
                !resultByRuleId.isEmpty() && resultByRuleId.values().stream().allMatch(RuleResult::satisfied);

        // courseType == null인 규칙이 졸업요건 규칙 — 미충족된 것만 사유로 노출
        List<String> unsatisfiedReasons = rules.stream()
                .filter(r -> r.courseType() == null)
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && !result.satisfied();
                })
                .map(GraduationRuleView::ruleName)
                .distinct()
                .toList();

        return new Summary(
                achievementRate,
                earnedCredits,
                targetCredits,
                remainingCredits,
                transcript.gpa(),
                graduated,
                unsatisfiedReasons);
    }

    private List<AreaOverview> buildAreaOverviews(
            List<GraduationRuleView> rules, Map<Long, RuleResult> resultByRuleId, EvaluationContext context) {
        Map<CourseType, List<GraduationRuleView>> rulesByCourseType = rules.stream()
                .filter(r -> r.courseType() != null)
                .collect(Collectors.groupingBy(GraduationRuleView::courseType));

        Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType = rules.stream()
                .filter(r -> r.courseType() != null && MIN_CREDITS.equals(r.typeName()))
                .collect(Collectors.groupingBy(GraduationRuleView::courseType));

        List<CourseType> relevantCourseTypes = new ArrayList<>();
        for (CourseType ct : CourseType.values()) {
            if (rulesByCourseType.containsKey(ct) || context.getTotalPassedCreditsByType(ct) > 0) {
                relevantCourseTypes.add(ct);
            }
        }

        return relevantCourseTypes.stream()
                .map(ct -> buildAreaOverview(ct, rulesByCourseType, minAreaRulesByCourseType, resultByRuleId, context))
                .toList();
    }

    private AreaOverview buildAreaOverview(
            CourseType courseType,
            Map<CourseType, List<GraduationRuleView>> rulesByCourseType,
            Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context) {

        List<GraduationRuleView> areaRules = rulesByCourseType.getOrDefault(courseType, List.of());
        List<GraduationRuleView> minAreaRules = minAreaRulesByCourseType.getOrDefault(courseType, List.of());

        long satisfiedCount = areaRules.stream()
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && result.satisfied();
                })
                .count();
        int achievementRate = areaRules.isEmpty() ? 100 : (int) (satisfiedCount * 100 / areaRules.size());

        boolean satisfied = areaRules.isEmpty()
                || areaRules.stream().allMatch(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && result.satisfied();
                });

        int earnedCredits = context.getTotalPassedCreditsByType(courseType);
        int remainingCredits = Math.max(0, resolveTypeTargetCredits(minAreaRules) - earnedCredits);

        return new AreaOverview(
                courseType.name(), courseTypeKoreanName(courseType), achievementRate, remainingCredits, satisfied);
    }

    private List<String> extractRequiredCodes(List<GraduationRuleView> rules) {
        List<String> codes = new ArrayList<>();
        for (GraduationRuleView rule : rules) {
            if (!"REQUIRED_COURSE".equals(rule.typeName())) continue;
            codes.addAll(parseStringList(rule.ruleConfig(), "courseCodes"));
        }
        return codes;
    }

    /**
     * 주어진 규칙 중 해당 학생에게 실제로 적용되는 REQUIRED_COURSE 규칙만 추린다.
     * 영어 레벨 면제(exemptEnglishLevels)·적용 대상(requiredEnglishLevels) 규칙을 반영한다.
     */
    private List<GraduationRuleView> applicableRequiredRules(
            List<GraduationRuleView> rules, String studentEnglishLevel) {
        List<GraduationRuleView> result = new ArrayList<>();
        for (GraduationRuleView rule : rules) {
            if (!"REQUIRED_COURSE".equals(rule.typeName())) continue;
            List<String> exemptLevels = parseStringList(rule.ruleConfig(), "exemptEnglishLevels");
            if (!exemptLevels.isEmpty() && studentEnglishLevel != null && exemptLevels.contains(studentEnglishLevel)) {
                continue;
            }
            List<String> requiredLevels = parseStringList(rule.ruleConfig(), "requiredEnglishLevels");
            if (!requiredLevels.isEmpty()
                    && (studentEnglishLevel == null || !requiredLevels.contains(studentEnglishLevel))) {
                continue;
            }
            result.add(rule);
        }
        return result;
    }

    private List<AreaDetailProjection.AreaSection> buildAreaSections(
            CourseType courseType,
            List<GraduationRuleView> areaRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            Map<String, CourseClassificationView> allCls,
            List<String> roleRequiredCodes) {

        String fallbackArea = courseTypeKoreanName(courseType);
        List<CourseRecordView> allPassed = context.getPassedCoursesByType(courseType);
        String studentEnglishLevel = context.getTranscript().englishLevel();

        // 이 이수구분에 속한 필수 규칙 — 미충족 시 규칙명·0학점 placeholder로 노출하기 위해 사용한다.
        record RequiredRule(GraduationRuleView rule, List<String> codes, String areaName) {}
        List<RequiredRule> requiredRules = new ArrayList<>();
        for (GraduationRuleView rule : applicableRequiredRules(areaRules, studentEnglishLevel)) {
            List<String> codes = parseStringList(rule.ruleConfig(), "courseCodes");
            // course_classification에 있으면 areaName을 직접 사용
            String ruleArea = codes.stream()
                    .map(allCls::get)
                    .filter(Objects::nonNull)
                    .map(CourseClassificationView::areaName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            // 미등록 과목이지만 사용자가 이수했다면 pdfAreaName에서 area를 추론
            if (ruleArea == null) {
                ruleArea = allPassed.stream()
                        .filter(cr -> cr.courseCode() != null && codes.contains(cr.courseCode()))
                        .findFirst()
                        .map(cr -> resolvePdfAreaName(cr.pdfAreaName(), courseType, null))
                        .orElse(null);
            }
            requiredRules.add(new RequiredRule(rule, codes, ruleArea));
        }

        // 단일 영역 목표학점만 섹션에 귀속시킨다.
        // 여러 영역을 합산하는 규칙은 어느 한 영역의 목표로 볼 수 없으므로 섹션 targetCredits는 0으로 두고,
        // 미충족 시 unsatisfiedReasons에 규칙명으로 노출된다.
        Map<String, Integer> targetCreditsByArea = new HashMap<>();
        for (GraduationRuleView rule : areaRules) {
            if (!isCreditTargetRule(rule)) continue;
            List<String> areaNames = parseStringList(rule.ruleConfig(), "areaNames");
            if (areaNames.size() == 1) {
                targetCreditsByArea.put(areaNames.get(0), parseIntField(rule.ruleConfig(), "minCredits", 0));
            }
        }

        LinkedHashMap<String, List<CourseRecordView>> coursesByArea = new LinkedHashMap<>();
        for (CourseRecordView cr : allPassed) {
            CourseClassificationView cls = cr.courseCode() != null ? allCls.get(cr.courseCode()) : null;
            String area;
            if (cls != null && cls.areaName() != null) {
                area = cls.areaName();
            } else {
                area = resolvePdfAreaName(cr.pdfAreaName(), courseType, fallbackArea);
            }
            coursesByArea.computeIfAbsent(area, k -> new ArrayList<>()).add(cr);
        }
        // 미충족 필수과목 영역도 섹션에 포함 (충족된 필수는 학생이 실제 이수한 영역에 자연히 표시됨)
        for (RequiredRule rr : requiredRules) {
            if (isRuleSatisfied(rr.rule(), resultByRuleId)) continue;
            String area = rr.areaName() != null ? rr.areaName() : fallbackArea;
            coursesByArea.computeIfAbsent(area, k -> new ArrayList<>());
        }

        List<AreaDetailProjection.AreaSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<CourseRecordView>> entry : coursesByArea.entrySet()) {
            String areaName = entry.getKey();
            List<CourseRecordView> areaCourses = entry.getValue();
            List<AreaDetailProjection.CourseItem> items = new ArrayList<>();

            // 이수 과목 분류 — 필수 학수번호를 충족시킨 과목은 학생 수강 내역대로 SATISFIED로 표시한다.
            // subCategory(실험, 개론 등)가 있으면 alias로 그룹핑, 없으면 개별 항목.
            List<CourseRecordView> requiredFulfilled = new ArrayList<>();
            LinkedHashMap<String, List<CourseRecordView>> bySubCategory = new LinkedHashMap<>();
            List<CourseRecordView> individualOptional = new ArrayList<>();
            for (CourseRecordView cr : areaCourses) {
                if (context.codeMatchesAny(cr.courseCode(), roleRequiredCodes)) {
                    requiredFulfilled.add(cr);
                    continue;
                }
                CourseClassificationView cls = cr.courseCode() != null ? allCls.get(cr.courseCode()) : null;
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
            for (CourseRecordView cr : requiredFulfilled) {
                items.add(new AreaDetailProjection.CourseItem(cr.courseName(), cr.credits(), "SATISFIED", null));
            }
            // 미충족 필수 과목 placeholder (규칙명·0학점) — 규칙이 속한 이수구분 탭에만 노출
            for (RequiredRule rr : requiredRules) {
                if (isRuleSatisfied(rr.rule(), resultByRuleId)) continue;
                String ruleArea = rr.areaName() != null ? rr.areaName() : fallbackArea;
                if (!areaName.equals(ruleArea)) continue;
                items.add(new AreaDetailProjection.CourseItem(
                        extractCourseTitle(rr.rule().ruleName()), 0, "UNSATISFIED", null));
            }
            for (Map.Entry<String, List<CourseRecordView>> subEntry : bySubCategory.entrySet()) {
                String subCat = subEntry.getKey();
                List<CourseRecordView> subCourses = subEntry.getValue();
                int subEarned =
                        subCourses.stream().mapToInt(CourseRecordView::credits).sum();
                List<String> detail =
                        subCourses.stream().map(CourseRecordView::courseName).toList();
                String subStatus = resolveSubCategoryStatus(subCat, areaRules, resultByRuleId);
                items.add(new AreaDetailProjection.CourseItem(subCat, subEarned, subStatus, detail));
            }
            for (CourseRecordView cr : individualOptional) {
                items.add(new AreaDetailProjection.CourseItem(cr.courseName(), cr.credits(), "OPTIONAL", null));
            }

            int earnedCredits =
                    areaCourses.stream().mapToInt(CourseRecordView::credits).sum();
            int targetCredits = targetCreditsByArea.getOrDefault(areaName, 0);

            boolean areaSatisfied = requiredRules.stream()
                    .filter(rr -> areaName.equals(rr.areaName() != null ? rr.areaName() : fallbackArea))
                    .allMatch(rr -> isRuleSatisfied(rr.rule(), resultByRuleId));
            if (targetCredits > 0) {
                areaSatisfied = areaSatisfied && earnedCredits >= targetCredits;
            }

            sections.add(
                    new AreaDetailProjection.AreaSection(areaName, earnedCredits, targetCredits, areaSatisfied, items));
        }

        return sections;
    }

    private static boolean isRuleSatisfied(GraduationRuleView rule, Map<Long, RuleResult> resultByRuleId) {
        RuleResult result = resultByRuleId.get(rule.id());
        return result != null && result.satisfied();
    }

    private static String extractCourseTitle(String ruleName) {
        for (String suffix : new String[] {"은 필수", "는 필수"}) {
            int idx = ruleName.indexOf(suffix);
            if (idx > 0) return ruleName.substring(0, idx);
        }
        return ruleName;
    }

    private static String resolvePdfAreaName(String pdfAreaName, CourseType courseType, String fallback) {
        if (pdfAreaName == null) return fallback;
        if (courseType == CourseType.FIRST_MAJOR || courseType == CourseType.SECOND_MAJOR) {
            return switch (pdfAreaName) {
                case "기초" -> "전공기초";
                case "전문" -> "전공전문";
                default -> pdfAreaName;
            };
        }
        return pdfAreaName;
    }

    /**
     * 소분류(alias) 항목의 상태를 결정한다.
     * 이 소분류를 대상으로 삼는 MIN_CREDITS(최소 이수량) 규칙이 있으면 그 판정 결과를 따르고,
     * 없으면 필수가 아니므로 OPTIONAL이다. 소분류별 분기를 하드코딩하지 않으므로 규칙이 늘어도 여기는 그대로다.
     */
    private String resolveSubCategoryStatus(
            String subCategory, List<GraduationRuleView> areaRules, Map<Long, RuleResult> resultByRuleId) {
        return areaRules.stream()
                .filter(r -> MIN_CREDITS.equals(r.typeName()))
                .filter(r -> parseStringList(r.ruleConfig(), "subCategories").contains(subCategory))
                .findFirst()
                .map(r -> isRuleSatisfied(r, resultByRuleId) ? "SATISFIED" : "UNSATISFIED")
                .orElse("OPTIONAL");
    }

    private AreaDetailProjection.CreditStatus buildTypeCredits(
            CourseType courseType, List<GraduationRuleView> areaRules, EvaluationContext context) {
        int earned = context.getTotalPassedCreditsByType(courseType);
        int target = resolveTypeTargetCredits(areaRules);
        return new AreaDetailProjection.CreditStatus(earned, target, Math.max(0, target - earned));
    }

    /**
     * 이수 구분 전체의 목표학점을 구한다.
     * 영역 제한이 없는 규칙이 있으면 그중 최대값을 쓰고(ex. 공통교양 17학점),
     * 없으면 영역별 목표학점을 합산한다(ex. 학문기초 기본소양 6 + MSC 21 = 27).
     */
    private int resolveTypeTargetCredits(List<GraduationRuleView> rules) {
        OptionalInt wholeType = rules.stream()
                .filter(this::isCreditTargetRule)
                .filter(r -> parseStringList(r.ruleConfig(), "areaNames").isEmpty())
                .mapToInt(r -> parseIntField(r.ruleConfig(), "minCredits", 0))
                .max();
        if (wholeType.isPresent()) return wholeType.getAsInt();

        return rules.stream()
                .filter(this::isCreditTargetRule)
                .filter(r -> !parseStringList(r.ruleConfig(), "areaNames").isEmpty())
                .mapToInt(r -> parseIntField(r.ruleConfig(), "minCredits", 0))
                .sum();
    }

    /**
     * 규칙의 minCredits를 리포트 목표학점으로 쓸 수 있는지 확인한다.
     * minCredits가 없는 규칙(ex. 실험 교과목 1과목 필수 선택 — minCount만 사용)은 목표학점이 아니다.
     */
    private boolean isCreditTargetRule(GraduationRuleView rule) {
        return MIN_CREDITS.equals(rule.typeName()) && hasNumberField(rule.ruleConfig(), "minCredits");
    }

    private List<String> parseStringList(String json, String field) {
        if (json == null) return List.of();
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode arr = node.path(field);
            if (arr.isNull() || arr.isMissingNode() || !arr.isArray()) return List.of();
            List<String> result = new ArrayList<>();
            arr.forEach(n -> result.add(n.asText()));
            return result;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** rule_config에 해당 숫자 필드가 실제로 들어 있는지 확인한다. */
    private boolean hasNumberField(String ruleConfig, String fieldName) {
        if (ruleConfig == null) return false;
        try {
            return MAPPER.readTree(ruleConfig).path(fieldName).isNumber();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private int parseIntField(String ruleConfig, String fieldName, int defaultValue) {
        if (ruleConfig == null) return defaultValue;
        try {
            JsonNode node = MAPPER.readTree(ruleConfig);
            JsonNode field = node.path(fieldName);
            return field.isMissingNode() ? defaultValue : field.asInt(defaultValue);
        } catch (JsonProcessingException e) {
            return defaultValue;
        }
    }

    private String courseTypeKoreanName(CourseType courseType) {
        return switch (courseType) {
            case COMMON_GENERAL -> "공통교양";
            case ACADEMIC_FOUNDATION -> "학문기초";
            case LIBERAL_ARTS -> "일반교양";
            case FIRST_MAJOR -> "제1전공";
            case SECOND_MAJOR -> "제2전공";
        };
    }
}
