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

    public GraduationReportResponse getReport(Long reportId) {
        TranscriptView transcript = transcriptLookupService
                .findById(reportId)
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

        // 미등록 과목: PDF course_type_name → courseType 추론 (areaName 등은 null)
        Map<String, CourseClassificationView> classificationByCourseCode = new HashMap<>(explicit);
        transcript.courseRecords().stream()
                .filter(cr -> cr.courseCode() != null)
                .filter(cr -> !explicit.containsKey(cr.courseCode()))
                .forEach(cr -> classificationByCourseCode.put(
                        cr.courseCode(),
                        new CourseClassificationView(inferCourseType(cr.courseTypeName()), null, null, null)));

        return new EvaluationContext(transcript, classificationByCourseCode);
    }

    /** PDF course_type_name 원시값을 CourseType으로 추론한다. 매핑 불가 시 null 반환. */
    private static CourseType inferCourseType(String courseTypeName) {
        if (courseTypeName == null) return null;
        return switch (courseTypeName) {
            case "공교" -> CourseType.COMMON_GENERAL;
            case "학기" -> CourseType.ACADEMIC_FOUNDATION;
            case "전공" -> CourseType.FIRST_MAJOR;
            case "일교" -> CourseType.LIBERAL_ARTS;
            default -> null;
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
                        boolean isNullArea =
                                node.path("areaNames").isNull() || node.path("areaNames").isMissingNode();
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
                .orElseGet(() -> parsedConfigs.stream().mapToInt(AreaRuleConfig::minCredits).sum());
        int remainingCredits = Math.max(0, targetCredits - earnedCredits);

        return new AreaOverview(
                courseType.name(), courseTypeKoreanName(courseType), achievementRate, remainingCredits, satisfied);
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
