package com.donggree.graduation.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CourseView;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.curriculum.RuleCategory;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduationReportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TranscriptLookupService transcriptLookupService;
    private final CurriculumLookupService curriculumLookupService;
    private final List<RuleEvaluator> evaluators;

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

        Summary summary = buildSummary(transcript, rules, resultByRuleId);
        List<AreaOverview> areaOverviews = buildAreaOverviews(rules, resultByRuleId, context);

        return new GraduationReportResponse(summary, areaOverviews);
    }

    private EvaluationContext buildContext(TranscriptView transcript) {
        List<String> courseCodes = transcript.courseRecords().stream()
                .map(CourseRecordView::courseCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, CourseView> courseByCode = curriculumLookupService.findCoursesByCodes(courseCodes);

        List<Long> courseIds =
                courseByCode.values().stream().map(CourseView::id).toList();

        Map<Long, CourseClassificationView> classificationByCourseId =
                curriculumLookupService.findCourseClassificationsByCourseIds(courseIds, transcript.admissionYear());

        Map<String, CourseClassificationView> classificationByCourseCode = courseCodes.stream()
                .filter(courseByCode::containsKey)
                .filter(code -> classificationByCourseId.containsKey(
                        courseByCode.get(code).id()))
                .collect(Collectors.toMap(
                        Function.identity(),
                        code -> classificationByCourseId.get(
                                courseByCode.get(code).id())));

        return new EvaluationContext(transcript, classificationByCourseCode);
    }

    private Map<Long, RuleResult> evaluateRules(List<GraduationRuleView> rules, EvaluationContext context) {
        Map<String, RuleEvaluator> evaluatorByTypeName =
                evaluators.stream().collect(Collectors.toMap(RuleEvaluator::supportedTypeName, Function.identity()));

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

        List<String> unsatisfiedReasons = rules.stream()
                .filter(r -> r.category() == RuleCategory.GRADUATION_REQ)
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
        // courseType별 MIN_AREA_CREDITS 규칙 그룹핑
        Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType = rules.stream()
                .filter(r -> "MIN_AREA_CREDITS".equals(r.typeName()))
                .collect(Collectors.groupingBy(r -> parseCourseType(r.ruleConfig())));

        // 표시할 courseType 결정: MIN_AREA_CREDITS 규칙이 있거나 이수 과목이 있는 것
        List<CourseType> relevantCourseTypes = new ArrayList<>();
        for (CourseType ct : CourseType.values()) {
            boolean hasRule = minAreaRulesByCourseType.containsKey(ct);
            boolean hasCredits = context.getTotalPassedCreditsByType(ct) > 0;
            if (hasRule || hasCredits) {
                relevantCourseTypes.add(ct);
            }
        }

        return relevantCourseTypes.stream()
                .map(courseType -> buildAreaOverview(courseType, minAreaRulesByCourseType, resultByRuleId, context))
                .toList();
    }

    private AreaOverview buildAreaOverview(
            CourseType courseType,
            Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context) {
        int earnedCredits = context.getTotalPassedCreditsByType(courseType);
        List<GraduationRuleView> areaRules = minAreaRulesByCourseType.getOrDefault(courseType, List.of());

        // targetCredits: subCategories=null인 규칙 우선, 없으면 가장 큰 값
        int targetCredits = areaRules.stream()
                .filter(r -> isNullSubCategories(r.ruleConfig()))
                .mapToInt(r -> parseIntField(r.ruleConfig(), "minCredits", 0))
                .max()
                .orElseGet(() -> areaRules.stream()
                        .mapToInt(r -> parseIntField(r.ruleConfig(), "minCredits", 0))
                        .sum());

        int remainingCredits = Math.max(0, targetCredits - earnedCredits);
        int achievementRate = targetCredits > 0 ? Math.min(100, earnedCredits * 100 / targetCredits) : 100;

        // 해당 courseType의 모든 MIN_AREA_CREDITS 규칙이 충족되어야 satisfied
        boolean satisfied = areaRules.isEmpty()
                || areaRules.stream().allMatch(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && result.satisfied();
                });

        return new AreaOverview(
                courseType.name(), courseTypeKoreanName(courseType), achievementRate, remainingCredits, satisfied);
    }

    private CourseType parseCourseType(String ruleConfig) {
        try {
            JsonNode node = MAPPER.readTree(ruleConfig);
            return CourseType.valueOf(node.path("courseType").asText());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("MIN_AREA_CREDITS ruleConfig에서 courseType 파싱 실패: " + ruleConfig, e);
        }
    }

    private boolean isNullSubCategories(String ruleConfig) {
        try {
            JsonNode node = MAPPER.readTree(ruleConfig);
            return node.path("subCategories").isNull()
                    || node.path("subCategories").isMissingNode();
        } catch (JsonProcessingException e) {
            return false;
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
