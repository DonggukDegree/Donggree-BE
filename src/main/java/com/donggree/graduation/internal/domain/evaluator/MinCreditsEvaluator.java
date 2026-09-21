package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 최소 이수량 규칙 평가기.
 * "지정한 과목 묶음에서 최소 N학점(또는 N과목)을 이수해야 한다"는 형태의 규칙을 모두 처리한다.
 * 구 MIN_AREA_CREDITS(영역 최소학점)와 구 SCIENCE_EXPERIMENT(실험 과목 필수 선택)를 흡수한 통합 타입이다.
 *
 * ruleConfig:
 *   {"courseType": "COMMON_GENERAL", "minCredits": 17}
 *   {"courseType": "COMMON_GENERAL", "areaNames": ["리더십"], "minCredits": 2}
 *   {"courseType": "ACADEMIC_FOUNDATION", "areaNames": ["수학", "과학"], "minCredits": 21}
 *   {"courseType": "ACADEMIC_FOUNDATION", "subCategories": ["실험"], "minCount": 1}
 *   {"courseType": "FIRST_MAJOR", "pdfAreaNames": ["전문"], "minCredits": 30}
 *   {"courseType": "FIRST_MAJOR", "pdfCourseTypeNames": ["전필"], "minCredits": 12}
 *   {"courseType": "FIRST_MAJOR", "pdfCourseTypeNames": ["전필"], "pdfAreaNames": ["전문"], "minCredits": 9}
 *   {"courseType": "SECOND_MAJOR", "minCredits": 36, "applicableMajorRoles": ["SECONDARY"]}
 *
 * 대상 과목 선택 — 조건(courseType·선택자)은 모두 AND로 좁힌다.
 * 값을 쓴 조건만 제약이 되고, 비어 있는(또는 null) 조건은 "제한 없음"이다.
 * 따라서 조건을 하나도 쓰지 않으면 이수한 전체 수강 이력이 대상이 된다.
 * 한 조건 안의 여러 값끼리는 OR다 — areaNames: ["수학", "과학"]은 수학이거나 과학.
 *
 * - courseType: course_classification 기준 이수구분.
 * - areaNames·subCategories: course_classification의 영역·소분류.
 * - pdfAreaNames: 성적표 PDF의 영역 원문("기초", "전문" 등). course_classification에 등록되지 않은
 *   전공 과목의 전공기초/전공전문 구분이 PDF에만 존재하므로 필요하다.
 * - pdfCourseTypeNames: 성적표 PDF의 이수구분 원문("전필", "전공", "공교", "학기", "일교", "자선").
 *   전공필수 여부는 PDF의 이수구분 칸에만 "전필"로 표시되므로("전필" 과목의 영역 원문은 "기초"/"전문"이다),
 *   전필 학점을 세려면 pdfAreaNames가 아니라 이 선택자를 써야 한다.
 * - courseCodes: 학수번호. "DAI*" 같은 prefix 패턴을 지원한다.
 *
 * 임계값(minCredits·minCount): 지정된 것을 모두 만족해야 충족(AND)이다. 둘 다 없으면 설정 오류다.
 */
@Component
public class MinCreditsEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "MIN_CREDITS";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        if (config.minCredits() == null && config.minCount() == null) {
            throw new IllegalStateException("MIN_CREDITS는 minCredits 또는 minCount가 필요합니다: " + rule.ruleConfig());
        }
        CourseType configuredCourseType = config.courseType() == null ? null : CourseType.valueOf(config.courseType());
        // 학과 세트의 기존 주전공 규칙을 복수전공 역할로 재사용한다.
        CourseType courseType =
                context.getMajorRole() == MajorRole.SECONDARY && configuredCourseType == CourseType.FIRST_MAJOR
                        ? CourseType.SECOND_MAJOR
                        : configuredCourseType;

        List<CourseRecordView> selected = context.getPassedCoursesForRule(rule.courseType()).stream()
                .filter(record -> matches(record, courseType, config, context))
                .toList();

        int earnedCredits =
                selected.stream().mapToInt(CourseRecordView::credits).sum();
        boolean satisfied = (config.minCredits() == null || earnedCredits >= config.minCredits())
                && (config.minCount() == null || selected.size() >= config.minCount());

        return new RuleResult(rule.ruleName(), satisfied);
    }

    /**
     * 수강 이력 하나가 이 규칙의 대상 과목 묶음에 속하는지 판단한다.
     * 값을 쓴 조건은 모두 만족해야 하고(AND), 비어 있는 조건은 제약이 아니다.
     */
    private boolean matches(CourseRecordView record, CourseType courseType, Config config, EvaluationContext context) {
        CourseClassificationView classification = context.getClassification(record);
        if (courseType != null && (classification == null || classification.courseType() != courseType)) {
            return false;
        }
        return matchesSelector(config.areaNames(), classification == null ? null : classification.areaName())
                && matchesSelector(config.subCategories(), classification == null ? null : classification.subCategory())
                && matchesSelector(config.pdfAreaNames(), record.pdfAreaName())
                && matchesSelector(config.pdfCourseTypeNames(), record.courseTypeName())
                && matchesCourseCodes(config.courseCodes(), record, context);
    }

    /** 선택자가 비어 있으면 제약 없음, 값이 있으면 그중 하나와 일치해야 한다. 값을 모르는 과목은 제외된다. */
    private static boolean matchesSelector(List<String> selector, String value) {
        return isEmpty(selector) || (value != null && selector.contains(value));
    }

    /** 학수번호 선택자는 prefix 패턴("DAI*")을 지원하므로 별도로 매칭한다. */
    private static boolean matchesCourseCodes(
            List<String> courseCodes, CourseRecordView record, EvaluationContext context) {
        return isEmpty(courseCodes) || context.codeMatchesAny(record.courseCode(), courseCodes);
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(
            String courseType,
            List<String> areaNames,
            List<String> subCategories,
            List<String> pdfAreaNames,
            List<String> pdfCourseTypeNames,
            List<String> courseCodes,
            Integer minCredits,
            Integer minCount) {}
}
