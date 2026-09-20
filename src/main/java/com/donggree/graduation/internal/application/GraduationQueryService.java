package com.donggree.graduation.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 학업 리포트 조회(Query) 응용 서비스.
 * graduation은 저장 애그리거트가 없는 무상태 판정 모듈이므로 쓰기(Command) 서비스는 없다.
 * 이 서비스는 오케스트레이션만 담당한다 — 타 모듈 조회(transcript·curriculum), 평가 컨텍스트 구성,
 * 규칙 평가(RuleEvaluator) 위임. 결과 조립(집계·영역 분류)은 {@link GraduationReportAssembler}에 위임한다.
 */
@Service
public class GraduationQueryService {

    private final TranscriptLookupService transcriptLookupService;
    private final CurriculumLookupService curriculumLookupService;
    private final GraduationReportAssembler reportAssembler;
    private final Map<String, RuleEvaluator> evaluatorByTypeName;

    public GraduationQueryService(
            TranscriptLookupService transcriptLookupService,
            CurriculumLookupService curriculumLookupService,
            List<RuleEvaluator> evaluators,
            GraduationReportAssembler reportAssembler) {
        this.transcriptLookupService = transcriptLookupService;
        this.curriculumLookupService = curriculumLookupService;
        this.reportAssembler = reportAssembler;
        this.evaluatorByTypeName =
                evaluators.stream().collect(Collectors.toMap(RuleEvaluator::supportedTypeName, Function.identity()));
    }

    public GraduationReportProjection getReport(Long memberId) {
        TranscriptView transcript = transcriptLookupService
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));

        // 학과·입학년도에 더해 과정(일반/심화)까지 맞는 세트를 고른다. 학생의 과정에 해당하는 세트가
        // 없으면 다른 과정의 요건으로 판정하지 않고 미지원 학과와 동일하게 실패시킨다.
        RequirementSetView requirementSet = curriculumLookupService
                .findActiveRequirementSet(
                        transcript.departmentId(), transcript.admissionYear(), transcript.engineeringCertified())
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));

        List<GraduationRuleView> rules = curriculumLookupService.findGraduationRules(requirementSet.id());
        EvaluationContext context = buildContext(transcript);
        Map<Long, RuleResult> resultByRuleId = evaluateRules(rules, context);

        return reportAssembler.assembleReport(
                transcript, rules, resultByRuleId, context, hasUnsupportedMajor(transcript));
    }

    private boolean hasUnsupportedMajor(TranscriptView transcript) {
        return transcript.dualMajor1Id() != null
                || transcript.dualMajor2Id() != null
                || transcript.subMajor1Id() != null
                || transcript.subMajor2Id() != null;
    }

    public AreaDetailProjection getAreaDetail(Long memberId, CourseType courseType) {
        TranscriptView transcript = transcriptLookupService
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));
        RequirementSetView requirementSet = curriculumLookupService
                .findActiveRequirementSet(
                        transcript.departmentId(), transcript.admissionYear(), transcript.engineeringCertified())
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));

        List<GraduationRuleView> allRules = curriculumLookupService.findGraduationRules(requirementSet.id());
        List<GraduationRuleView> areaRules =
                allRules.stream().filter(r -> courseType.equals(r.courseType())).toList();

        EvaluationContext context = buildContext(transcript);
        Map<Long, RuleResult> resultByRuleId = evaluateRules(areaRules, context);

        // REQUIRED_COURSE rule의 과목이 미이수된 경우에도 areaName을 알기 위해 별도 조회
        List<String> requiredCodes = reportAssembler.requiredCourseCodes(areaRules);
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
        List<String> globalRequiredCodes =
                reportAssembler.applicableRequiredCourseCodes(allRules, transcript.englishLevel());

        return reportAssembler.assembleAreaDetail(
                courseType, areaRules, resultByRuleId, context, allCls, globalRequiredCodes);
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
}
