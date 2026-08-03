package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
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
 *
 * - courseType: null이면 이수구분 제한 없이 전체 수강 이력을 대상으로 한다.
 * - 선택자(areaNames·subCategories·courseCodes): 지정된 것들의 합집합(OR)이 대상 과목이 된다.
 *   셋 다 비어 있으면 courseType 전체가 대상이다. courseCodes는 "DAI*" 같은 prefix 패턴을 지원한다.
 * - 임계값(minCredits·minCount): 지정된 것을 모두 만족해야 충족(AND)이다. 둘 다 없으면 설정 오류다.
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
        CourseType courseType = config.courseType() == null ? null : CourseType.valueOf(config.courseType());

        List<CourseRecordView> selected = context.getPassedCourses().stream()
                .filter(record -> matches(record, courseType, config, context))
                .toList();

        int earnedCredits =
                selected.stream().mapToInt(CourseRecordView::credits).sum();
        boolean satisfied = (config.minCredits() == null || earnedCredits >= config.minCredits())
                && (config.minCount() == null || selected.size() >= config.minCount());

        return new RuleResult(rule.ruleName(), satisfied);
    }

    /** 수강 이력 하나가 이 규칙의 대상 과목 묶음에 속하는지 판단한다. */
    private boolean matches(CourseRecordView record, CourseType courseType, Config config, EvaluationContext context) {
        CourseClassificationView classification = context.getClassification(record.courseCode());
        if (courseType != null && (classification == null || classification.courseType() != courseType)) {
            return false;
        }
        if (isEmpty(config.areaNames()) && isEmpty(config.subCategories()) && isEmpty(config.courseCodes())) {
            return true;
        }
        if (classification != null
                && classification.areaName() != null
                && !isEmpty(config.areaNames())
                && config.areaNames().contains(classification.areaName())) {
            return true;
        }
        if (classification != null
                && classification.subCategory() != null
                && !isEmpty(config.subCategories())
                && config.subCategories().contains(classification.subCategory())) {
            return true;
        }
        return !isEmpty(config.courseCodes()) && context.codeMatchesAny(record.courseCode(), config.courseCodes());
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(
            String courseType,
            List<String> areaNames,
            List<String> subCategories,
            List<String> courseCodes,
            Integer minCredits,
            Integer minCount) {}
}
