package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 선이수체계 규칙 평가기. 전공 선이수·영어 선이수(EAS) 등에 공통으로 사용한다.
 * ruleConfig:
 *   {"targetCourseCodes": ["CS_자료구조"], "prerequisiteCourseCodes": ["CS_기초프로그래밍"],
 *    "conditionField": null, "conditionValue": null}
 *
 * 단일 코드면 1개짜리 배열, 동일유사 교과목이면 여러 코드 배열.
 * 평가 로직:
 *   - conditionField가 있으면 해당 조건이 맞는 학생에게만 적용한다 (ex. englishLevel = "S4").
 *   - target 그룹 과목을 수강하지 않았으면 선이수 위반 없음 → 충족.
 *   - target을 수강했지만 prerequisite 그룹 과목을 수강하지 않았으면 → 미충족.
 *   - target 학기보다 prerequisite 학기가 이전이어야 충족이다 (같은 학기는 미충족).
 */
@Component
public class PrerequisiteEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "PREREQUISITE";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);

        if (!appliesToStudent(config, context)) {
            return new RuleResult(rule.ruleName(), true);
        }

        Optional<String> targetSemester = context.getEarliestSemesterByAnyCourseCode(config.targetCourseCodes());
        if (targetSemester.isEmpty()) {
            return new RuleResult(rule.ruleName(), true);
        }

        Optional<String> prereqSemester = context.getEarliestSemesterByAnyCourseCode(config.prerequisiteCourseCodes());
        if (prereqSemester.isEmpty()) {
            return new RuleResult(rule.ruleName(), false);
        }

        int prereqOrdinal = EvaluationContext.semesterOrdinal(prereqSemester.get());
        int targetOrdinal = EvaluationContext.semesterOrdinal(targetSemester.get());
        boolean satisfied = prereqOrdinal > 0 && targetOrdinal > 0 && prereqOrdinal < targetOrdinal;
        return new RuleResult(rule.ruleName(), satisfied);
    }

    private boolean appliesToStudent(Config config, EvaluationContext context) {
        if (config.conditionField() == null) return true;
        if ("englishLevel".equals(config.conditionField())) {
            return config.conditionValue().equals(context.getTranscript().englishLevel());
        }
        return true;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(
            List<String> targetCourseCodes,
            List<String> prerequisiteCourseCodes,
            String conditionField,
            String conditionValue) {}
}
