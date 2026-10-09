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
import com.donggree.graduation.internal.application.projection.GraduationReportProjection.AdditionalNotice;
import com.donggree.graduation.internal.application.projection.ReportPreviewProjection;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.graduation.internal.domain.MajorRoleRuleMatcher;
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
        return assembleReport(evaluateTranscript(findTranscript(memberId)));
    }

    public AreaDetailProjection getAreaDetail(Long memberId, CourseType courseType) {
        return assembleAreaDetail(courseType, evaluateTranscript(findTranscript(memberId)));
    }

    /** 임시 성적표도 사용자와 같은 판정·조립 경로를 사용하며, 저장 성적표는 조회하지 않는다. */
    public ReportPreviewProjection preview(TranscriptView transcript) {
        ReportEvaluation evaluation = evaluateTranscript(transcript);
        GraduationReportProjection report = assembleReport(evaluation);
        Map<CourseType, AreaDetailProjection> details = new LinkedHashMap<>();
        for (var area : report.areaOverviews()) {
            CourseType courseType = CourseType.valueOf(area.courseType());
            details.put(courseType, assembleAreaDetail(courseType, evaluation));
        }
        return new ReportPreviewProjection(report, details);
    }

    private TranscriptView findTranscript(Long memberId) {
        return transcriptLookupService
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REPORT_NOT_FOUND));
    }

    /** 한 번의 미리보기에서는 세트 선택과 판정을 한 번만 수행해 요약·상세에 같은 결과를 사용한다. */
    private ReportEvaluation evaluateTranscript(TranscriptView transcript) {
        // 미식별 학과 성적표는 조회·관리만 허용하고 다른 학과의 세트로 대신 판정하지 않는다.
        if (transcript.departmentId() == null) {
            throw new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND);
        }
        Map<String, CourseClassificationView> classifications = buildClassifications(transcript);
        ResolvedRules resolved = resolveScopedRules(transcript, classifications);
        List<ScopedRules> scopes = resolved.scopes();
        List<GraduationRuleView> rules =
                scopes.stream().flatMap(scope -> scope.rules().stream()).toList();
        return new ReportEvaluation(
                transcript,
                rules,
                // 사유·탭 상태도 규칙의 이수구분을 사용한다. 전공만 역할별 제1·제2전공으로 구분한다.
                rules,
                evaluateScopes(scopes),
                EvaluationContext.report(transcript, classifications),
                requiresAccuracyWarning(transcript, resolved.dualMajor1Evaluated()),
                resolved.additionalNotices());
    }

    private GraduationReportProjection assembleReport(ReportEvaluation evaluation) {
        return reportAssembler.assembleReport(
                evaluation.transcript(),
                evaluation.rules(),
                evaluation.statusRules(),
                evaluation.results(),
                evaluation.context(),
                evaluation.accuracyWarning(),
                evaluation.additionalNotices());
    }

    private boolean requiresAccuracyWarning(TranscriptView transcript, boolean dualMajor1Evaluated) {
        return (transcript.dualMajor1Id() != null && !dualMajor1Evaluated)
                || transcript.dualMajor2Id() != null
                || transcript.subMajor1Id() != null
                || transcript.subMajor2Id() != null
                || transcript.unresolvedAdditionalMajor()
                || transcript.transfer();
    }

    private AreaDetailProjection assembleAreaDetail(CourseType courseType, ReportEvaluation evaluation) {
        TranscriptView transcript = evaluation.transcript();
        List<GraduationRuleView> allRules = evaluation.rules();
        List<GraduationRuleView> areaRules =
                allRules.stream().filter(r -> courseType.equals(r.courseType())).toList();
        List<GraduationRuleView> statusRules = evaluation.statusRules().stream()
                .filter(r -> courseType.equals(r.courseType()))
                .toList();
        Map<Long, RuleResult> resultByRuleId = evaluation.results();
        EvaluationContext context = evaluation.context();

        // REQUIRED_COURSE rule의 과목이 미이수된 경우에도 areaName을 알기 위해 별도 조회
        List<String> requiredCodes = reportAssembler.requiredCourseCodes(areaRules);
        Map<String, CourseClassificationView> supplementalCls = requiredCodes.isEmpty()
                ? Map.of()
                : curriculumLookupService.findCourseClassifications(requiredCodes, transcript.admissionYear());

        Map<String, CourseClassificationView> allCls = new HashMap<>(supplementalCls);
        context.getPassedCoursesByType(courseType).stream()
                .filter(cr -> cr.courseCode() != null)
                .forEach(cr -> {
                    CourseClassificationView cls = context.getClassification(cr);
                    if (cls != null) allCls.put(cr.courseCode(), cls);
                });

        // 필수 규칙은 다른 이수구분에 속하더라도 학수번호만 이수하면 충족이다.
        // 전체 규칙 중 학생에게 적용되는 REQUIRED_COURSE 코드 패턴을 모아, 학생 PDF 분류 탭에서
        // 해당 과목을 충족(SATISFIED)으로 표시하기 위한 기준으로 사용한다.
        List<String> roleRequiredCodes =
                reportAssembler.applicableRequiredCourseCodes(allRules, transcript.englishLevel(), courseType);

        return reportAssembler.assembleAreaDetail(
                courseType, areaRules, statusRules, resultByRuleId, context, allCls, roleRequiredCodes);
    }

    private Map<String, CourseClassificationView> buildClassifications(TranscriptView transcript) {
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

        return classificationByCourseCode;
    }

    // course_classification 미등록 과목의 courseType/areaName을 PDF 이수구분으로 추론한다.
    // 테스트에서 직접 검증하기 위해 package-private으로 노출한다.
    static CourseClassificationView inferClassification(String courseTypeName) {
        if (courseTypeName == null) return new CourseClassificationView(null, null, null, null);
        return switch (courseTypeName) {
            case "공교" -> new CourseClassificationView(CourseType.COMMON_GENERAL, null, null, null);
            case "학기" -> new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, null, null, null);
            case "전공", "전필" -> new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null);
            case "복수1", "복수2" -> new CourseClassificationView(CourseType.SECOND_MAJOR, null, null, null);
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

    private ResolvedRules resolveScopedRules(
            TranscriptView transcript, Map<String, CourseClassificationView> classifications) {
        List<ScopedRules> scopes = new java.util.ArrayList<>();
        List<AdditionalNotice> notices = new java.util.ArrayList<>();
        MajorRole primaryRole = hasDualMajor(transcript) ? MajorRole.DUAL_PRIMARY : MajorRole.SINGLE_PRIMARY;

        RequirementSetView primarySet = findRequirementSet(
                transcript.departmentId(), transcript.admissionYear(), transcript.engineeringCertified());
        addNotice(notices, primarySet);
        List<GraduationRuleView> primaryRules = filterRules(primarySet.id(), primaryRole);
        scopes.add(new ScopedRules(primaryRules, EvaluationContext.primary(transcript, classifications, primaryRole)));

        boolean dualMajor1Evaluated = addSecondaryScope(scopes, transcript, classifications, notices);
        return new ResolvedRules(scopes, dualMajor1Evaluated, List.copyOf(notices));
    }

    private boolean addSecondaryScope(
            List<ScopedRules> scopes,
            TranscriptView transcript,
            Map<String, CourseClassificationView> classifications,
            List<AdditionalNotice> notices) {
        if (transcript.dualMajor1Id() == null) return true;

        // 복수전공은 주전공의 공학인증 심화과정이 아니므로 대상 학과의 일반과정 세트를 사용한다.
        RequirementSetView secondarySet = curriculumLookupService
                .findActiveRequirementSet(transcript.dualMajor1Id(), transcript.admissionYear(), false)
                .orElse(null);
        if (secondarySet == null) return false;

        List<GraduationRuleView> secondaryRules = filterRules(secondarySet.id(), MajorRole.SECONDARY).stream()
                .map(this::asSecondaryMajorRule)
                .toList();
        if (secondaryRules.isEmpty()) return false;

        addNotice(notices, secondarySet);
        scopes.add(new ScopedRules(secondaryRules, EvaluationContext.secondary(transcript, classifications, "복수1")));
        return true;
    }

    private void addNotice(List<AdditionalNotice> notices, RequirementSetView set) {
        if (set.studentNotice() == null || set.studentNotice().isBlank()) return;
        notices.add(new AdditionalNotice(
                set.id(),
                set.collegeName(),
                set.departmentName(),
                set.track(),
                set.yearStart(),
                set.yearEnd(),
                set.studentNotice()));
    }

    private RequirementSetView findRequirementSet(Long departmentId, int admissionYear, boolean engineeringCertified) {
        return curriculumLookupService
                .findActiveRequirementSet(departmentId, admissionYear, engineeringCertified)
                .orElseThrow(() -> new GeneralException(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
    }

    private List<GraduationRuleView> filterRules(Long requirementSetId, MajorRole role) {
        return curriculumLookupService.findGraduationRules(requirementSetId).stream()
                .filter(rule -> MajorRoleRuleMatcher.applies(rule, role))
                .toList();
    }

    private GraduationRuleView asSecondaryMajorRule(GraduationRuleView rule) {
        CourseType displayType =
                rule.courseType() == CourseType.FIRST_MAJOR ? CourseType.SECOND_MAJOR : rule.courseType();
        // 주전공 규칙과 ID가 겹치지 않도록 리포트 조립에만 쓰이는 음수 스코프 ID를 부여한다.
        long scopedRuleId = -(1_000_000_000L + rule.id());
        // 학문기초·교양은 원래 탭, 이수구분 없는 일반 요건은 요약에 표시한다.
        // 역할 접두어 없이 규칙명을 유지해 같은 위치의 미충족 문구만 중복 제거한다.
        return new GraduationRuleView(scopedRuleId, rule.typeName(), displayType, rule.ruleName(), rule.ruleConfig());
    }

    private Map<Long, RuleResult> evaluateScopes(List<ScopedRules> scopes) {
        Map<Long, RuleResult> results = new LinkedHashMap<>();
        for (ScopedRules scope : scopes) {
            results.putAll(evaluateRules(scope.rules(), scope.context()));
        }
        return results;
    }

    private boolean hasDualMajor(TranscriptView transcript) {
        return transcript.dualMajorDeclared() || transcript.dualMajor1Id() != null || transcript.dualMajor2Id() != null;
    }

    private record ReportEvaluation(
            TranscriptView transcript,
            List<GraduationRuleView> rules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> results,
            EvaluationContext context,
            boolean accuracyWarning,
            List<AdditionalNotice> additionalNotices) {}

    private record ScopedRules(List<GraduationRuleView> rules, EvaluationContext context) {}

    private record ResolvedRules(
            List<ScopedRules> scopes, boolean dualMajor1Evaluated, List<AdditionalNotice> additionalNotices) {}
}
