package com.donggree.graduation.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.AreaOverview;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse.Summary;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GraduationReportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TranscriptLookupService transcriptLookupService;
    private final CurriculumLookupService curriculumLookupService;
    private final Map<String, RuleEvaluator> evaluatorByTypeName;

    public GraduationReportService(
            TranscriptLookupService transcriptLookupService,
            CurriculumLookupService curriculumLookupService,
            List<RuleEvaluator> evaluators) {
        this.transcriptLookupService = transcriptLookupService;
        this.curriculumLookupService = curriculumLookupService;
        this.evaluatorByTypeName =
                evaluators.stream().collect(Collectors.toMap(RuleEvaluator::supportedTypeName, Function.identity()));
    }

    public GraduationReportResponse getReport(Long memberId) {
        TranscriptView transcript = transcriptLookupService
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));

        RequirementSetView requirementSet = curriculumLookupService
                .findActiveRequirementSet(transcript.departmentId(), transcript.admissionYear())
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));

        List<GraduationRuleView> rules = curriculumLookupService.findGraduationRules(requirementSet.id());
        EvaluationContext context = buildContext(transcript);
        Map<Long, RuleResult> resultByRuleId = evaluateRules(rules, context);

        return new GraduationReportResponse(
                buildSummary(transcript, rules, resultByRuleId), buildAreaOverviews(rules, resultByRuleId, context));
    }

    private EvaluationContext buildContext(TranscriptView transcript) {
        List<String> courseCodes = transcript.courseRecords().stream()
                .map(CourseRecordView::courseCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // course_classification에 등록된 과목: 명시적 분류 사용
        Map<String, CourseClassificationView> explicit =
                curriculumLookupService.findCourseClassifications(courseCodes, transcript.admissionYear());

        // 미등록 과목: PDF course_type_name → courseType/areaName 추론
        Map<String, CourseClassificationView> classificationByCourseCode = new HashMap<>(explicit);
        transcript.courseRecords().stream()
                .filter(cr -> cr.courseCode() != null)
                .filter(cr -> !explicit.containsKey(cr.courseCode()))
                .forEach(cr ->
                        classificationByCourseCode.put(cr.courseCode(), inferClassification(cr.courseTypeName())));

        return new EvaluationContext(transcript, classificationByCourseCode);
    }

    // course_classification 미등록 과목의 courseType/areaName을 PDF 이수구분으로 추론한다.
    // 테스트에서 직접 검증하기 위해 package-private으로 노출한다.
    static CourseClassificationView inferClassification(String courseTypeName) {
        if (courseTypeName == null) return new CourseClassificationView(null, null, null, null);
        return switch (courseTypeName) {
            case "공교" -> new CourseClassificationView(CourseType.COMMON_GENERAL, null, null, null);
            case "학기" -> new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, null, null, null);
            case "전공", "전필" -> new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null);
            case "일교" -> new CourseClassificationView(CourseType.LIBERAL_ARTS, "일반교양", null, null);
            case "자선" -> new CourseClassificationView(CourseType.LIBERAL_ARTS, "자유선택", null, null);
            default -> new CourseClassificationView(null, null, null, null);
        };
    }

    private Map<Long, RuleResult> evaluateRules(List<GraduationRuleView> rules, EvaluationContext context) {
        Map<Long, RuleResult> results = new LinkedHashMap<>();
        for (GraduationRuleView rule : rules) {
            RuleEvaluator evaluator = evaluatorByTypeName.get(rule.typeName());
            if (evaluator != null) {
                results.put(rule.id(), evaluator.evaluate(rule, context));
            }
        }
        return results;
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
                .filter(r -> r.courseType() != null && "MIN_AREA_CREDITS".equals(r.typeName()))
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

        // MIN_AREA_CREDITS rule_config를 한 번만 파싱하여 재사용
        record AreaRuleConfig(boolean isNullArea, int minCredits) {}
        List<AreaRuleConfig> parsedConfigs = minAreaRules.stream()
                .map(r -> {
                    try {
                        JsonNode node = MAPPER.readTree(r.ruleConfig());
                        boolean isNullArea = node.path("areaNames").isNull()
                                || node.path("areaNames").isMissingNode();
                        int credits = node.path("minCredits").asInt(0);
                        return new AreaRuleConfig(isNullArea, credits);
                    } catch (JsonProcessingException e) {
                        return new AreaRuleConfig(false, 0);
                    }
                })
                .toList();

        int earnedCredits = context.getTotalPassedCreditsByType(courseType);
        int targetCredits = parsedConfigs.stream()
                .filter(AreaRuleConfig::isNullArea)
                .mapToInt(AreaRuleConfig::minCredits)
                .max()
                .orElseGet(() -> parsedConfigs.stream()
                        .mapToInt(AreaRuleConfig::minCredits)
                        .sum());
        int remainingCredits = Math.max(0, targetCredits - earnedCredits);

        return new AreaOverview(
                courseType.name(), courseTypeKoreanName(courseType), achievementRate, remainingCredits, satisfied);
    }

    public AreaDetailResponse getAreaDetail(Long memberId, CourseType courseType) {
        TranscriptView transcript = transcriptLookupService
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));
        RequirementSetView requirementSet = curriculumLookupService
                .findActiveRequirementSet(transcript.departmentId(), transcript.admissionYear())
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));

        List<GraduationRuleView> allRules = curriculumLookupService.findGraduationRules(requirementSet.id());
        List<GraduationRuleView> areaRules =
                allRules.stream().filter(r -> courseType.equals(r.courseType())).toList();

        EvaluationContext context = buildContext(transcript);
        Map<Long, RuleResult> resultByRuleId = evaluateRules(areaRules, context);

        // REQUIRED_COURSE rule의 과목이 미이수된 경우에도 areaName을 알기 위해 별도 조회
        List<String> requiredCodes = extractRequiredCodes(areaRules);
        Map<String, CourseClassificationView> supplementalCls = requiredCodes.isEmpty()
                ? Map.of()
                : curriculumLookupService.findCourseClassifications(requiredCodes, transcript.admissionYear());

        Map<String, CourseClassificationView> allCls = new HashMap<>(supplementalCls);
        context.getPassedCoursesByType(courseType).stream()
                .filter(cr -> cr.courseCode() != null)
                .forEach(cr -> {
                    CourseClassificationView cls = context.getClassification(cr.courseCode());
                    if (cls != null) allCls.put(cr.courseCode(), cls);
                });

        // 필수 규칙은 다른 이수구분에 속하더라도 학수번호만 이수하면 충족이다.
        // 전체 규칙 중 학생에게 적용되는 REQUIRED_COURSE 코드 패턴을 모아, 학생 PDF 분류 탭에서
        // 해당 과목을 충족(SATISFIED)으로 표시하기 위한 기준으로 사용한다.
        List<String> globalRequiredCodes = applicableRequiredRules(allRules, transcript.englishLevel()).stream()
                .flatMap(r -> parseStringList(r.ruleConfig(), "courseCodes").stream())
                .toList();

        List<AreaDetailResponse.AreaSection> areaDetails =
                buildAreaSections(courseType, areaRules, resultByRuleId, context, allCls, globalRequiredCodes);

        List<String> unsatisfiedReasons = areaRules.stream()
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && !result.satisfied();
                })
                .map(GraduationRuleView::ruleName)
                .distinct()
                .toList();

        AreaDetailResponse.CreditStatus creditStatus = buildTypeCredits(courseType, areaRules, context);

        return new AreaDetailResponse(areaDetails, unsatisfiedReasons, creditStatus);
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

    private List<AreaDetailResponse.AreaSection> buildAreaSections(
            CourseType courseType,
            List<GraduationRuleView> areaRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            Map<String, CourseClassificationView> allCls,
            List<String> globalRequiredCodes) {

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

        // areaNames가 단일 영역인 MIN_AREA_CREDITS rule → 해당 영역의 targetCredits
        Map<String, Integer> targetCreditsByArea = new HashMap<>();
        for (GraduationRuleView rule : areaRules) {
            if (!"MIN_AREA_CREDITS".equals(rule.typeName())) continue;
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

        List<AreaDetailResponse.AreaSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<CourseRecordView>> entry : coursesByArea.entrySet()) {
            String areaName = entry.getKey();
            List<CourseRecordView> areaCourses = entry.getValue();
            List<AreaDetailResponse.CourseItem> items = new ArrayList<>();

            // 이수 과목 분류 — 필수 학수번호를 충족시킨 과목은 학생 수강 내역대로 SATISFIED로 표시한다.
            // subCategory(실험, 개론 등)가 있으면 alias로 그룹핑, 없으면 개별 항목.
            List<CourseRecordView> requiredFulfilled = new ArrayList<>();
            LinkedHashMap<String, List<CourseRecordView>> bySubCategory = new LinkedHashMap<>();
            List<CourseRecordView> individualOptional = new ArrayList<>();
            for (CourseRecordView cr : areaCourses) {
                if (context.codeMatchesAny(cr.courseCode(), globalRequiredCodes)) {
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
                items.add(new AreaDetailResponse.CourseItem(cr.courseName(), cr.credits(), "SATISFIED", null));
            }
            // 미충족 필수 과목 placeholder (규칙명·0학점) — 규칙이 속한 이수구분 탭에만 노출
            for (RequiredRule rr : requiredRules) {
                if (isRuleSatisfied(rr.rule(), resultByRuleId)) continue;
                String ruleArea = rr.areaName() != null ? rr.areaName() : fallbackArea;
                if (!areaName.equals(ruleArea)) continue;
                items.add(new AreaDetailResponse.CourseItem(
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
                items.add(new AreaDetailResponse.CourseItem(subCat, subEarned, subStatus, detail));
            }
            for (CourseRecordView cr : individualOptional) {
                items.add(new AreaDetailResponse.CourseItem(cr.courseName(), cr.credits(), "OPTIONAL", null));
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
                    new AreaDetailResponse.AreaSection(areaName, earnedCredits, targetCredits, areaSatisfied, items));
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

    private static String resolveSubCategoryStatus(
            String subCategory, List<GraduationRuleView> areaRules, Map<Long, RuleResult> resultByRuleId) {
        if ("실험".equals(subCategory)) {
            return areaRules.stream()
                    .filter(r -> "SCIENCE_EXPERIMENT".equals(r.typeName()))
                    .findFirst()
                    .map(r -> {
                        RuleResult result = resultByRuleId.get(r.id());
                        return (result != null && result.satisfied()) ? "SATISFIED" : "UNSATISFIED";
                    })
                    .orElse("OPTIONAL");
        }
        return "OPTIONAL";
    }

    private AreaDetailResponse.CreditStatus buildTypeCredits(
            CourseType courseType, List<GraduationRuleView> areaRules, EvaluationContext context) {
        int earned = context.getTotalPassedCreditsByType(courseType);
        List<GraduationRuleView> minAreaRules = areaRules.stream()
                .filter(r -> "MIN_AREA_CREDITS".equals(r.typeName()))
                .toList();

        record ParsedMinArea(boolean isWholeType, int minCredits) {}
        // ruleConfig JSON을 규칙당 한 번만 파싱해 areaNames 유무와 minCredits를 추출한다.
        List<ParsedMinArea> parsedList = minAreaRules.stream()
                .map(r -> {
                    try {
                        JsonNode node = MAPPER.readTree(r.ruleConfig());
                        JsonNode areaNames = node.path("areaNames");
                        boolean isWhole = !areaNames.isArray() || areaNames.isEmpty();
                        int credits = node.path("minCredits").asInt(0);
                        return new ParsedMinArea(isWhole, credits);
                    } catch (JsonProcessingException e) {
                        return new ParsedMinArea(false, 0);
                    }
                })
                .toList();

        // areaNames=null 규칙이 있으면 그 값 사용, 없으면 모든 MIN_AREA_CREDITS 합산
        OptionalInt wholeType = parsedList.stream()
                .filter(ParsedMinArea::isWholeType)
                .mapToInt(ParsedMinArea::minCredits)
                .max();
        int target = wholeType.isPresent()
                ? wholeType.getAsInt()
                : parsedList.stream().mapToInt(ParsedMinArea::minCredits).sum();

        return new AreaDetailResponse.CreditStatus(earned, target, Math.max(0, target - earned));
    }

    private List<String> parseStringList(String json, String field) {
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

    private int parseIntField(String ruleConfig, String fieldName, int defaultValue) {
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
