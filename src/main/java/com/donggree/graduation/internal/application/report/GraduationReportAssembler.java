package com.donggree.graduation.internal.application.report;

import static com.donggree.graduation.internal.application.report.GraduationAreaDetailAssembler.buildAreaSections;
import static com.donggree.graduation.internal.application.report.GraduationSummaryAssembler.buildAreaOverviews;
import static com.donggree.graduation.internal.application.report.GraduationSummaryAssembler.buildSummary;
import static com.donggree.graduation.internal.application.report.ReportCreditTargets.buildTypeCredits;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.applicableRequiredRules;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.extractRequiredCodes;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.requiredCodes;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.TranscriptView;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 조회 결과를 요약·상세 조립기에 위임한다. 외부 모듈 조회와 실제 규칙 판정은 수행하지 않는다. */
@Component
public class GraduationReportAssembler {

    public GraduationReportProjection assembleReport(
            TranscriptView transcript,
            List<GraduationRuleView> rules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            boolean hasUnsupportedMajor) {
        return assembleReport(transcript, rules, statusRules, resultByRuleId, context, hasUnsupportedMajor, List.of());
    }

    public GraduationReportProjection assembleReport(
            TranscriptView transcript,
            List<GraduationRuleView> rules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            boolean hasUnsupportedMajor,
            List<GraduationReportProjection.AdditionalNotice> additionalNotices) {
        return new GraduationReportProjection(
                buildSummary(transcript, rules, statusRules, resultByRuleId),
                buildAreaOverviews(rules, statusRules, resultByRuleId, context),
                hasUnsupportedMajor,
                transcript.englishPassResult(),
                additionalNotices);
    }

    public AreaDetailProjection assembleAreaDetail(
            CourseType courseType,
            List<GraduationRuleView> areaRules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context,
            Map<String, CourseClassificationView> allCls,
            List<String> roleRequiredCodes) {

        List<AreaDetailProjection.AreaSection> areaDetails =
                buildAreaSections(courseType, areaRules, resultByRuleId, context, allCls, roleRequiredCodes);

        List<String> unsatisfiedReasons = statusRules.stream()
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && !result.satisfied();
                })
                .map(GraduationRuleView::ruleName)
                // 학과별 판정은 유지하고, 이 탭의 미충족 문구만 한 번 표시한다.
                .distinct()
                .toList();

        AreaDetailProjection.CreditStatus creditStatus = buildTypeCredits(courseType, areaRules, context);

        return new AreaDetailProjection(areaDetails, unsatisfiedReasons, creditStatus);
    }

    public List<String> requiredCourseCodes(List<GraduationRuleView> rules) {
        return extractRequiredCodes(rules);
    }

    public List<String> applicableRequiredCourseCodes(
            List<GraduationRuleView> rules, String studentEnglishLevel, CourseType displayedCourseType) {
        boolean secondaryArea = displayedCourseType == CourseType.SECOND_MAJOR;
        return applicableRequiredRules(rules, studentEnglishLevel).stream()
                // 복수전공 규칙은 GraduationQueryService에서 음수 스코프 ID를 부여한다.
                // 역할이 다른 같은 학수번호가 상세 화면에서 잘못 충족 표시되는 것을 막는다.
                .filter(rule -> secondaryArea == (rule.id() < 0)
                        // 복수전공 학문기초·공통교양 필수는 공통 이수 내역에서도 충족 가능하다.
                        || (rule.id() < 0
                                && (rule.courseType() == CourseType.ACADEMIC_FOUNDATION
                                        || rule.courseType() == CourseType.COMMON_GENERAL)))
                .flatMap(r -> requiredCodes(r).stream())
                .distinct()
                .toList();
    }
}
