package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 필수 과목 이수 규칙 평가기. 수학필수·전공필수·공통교양필수 등에 공통으로 사용한다.
 * ruleConfig: {"courseCodes": ["PRI4001"]}
 *   단일 코드면 1개짜리 배열, 동일유사 교과목이면 여러 코드 배열 — 하나라도 이수하면 충족.
 * exemptEnglishLevels: 해당 영어 레벨 학생은 규칙 면제(자동 충족). ex. EAS 규칙에서 S0 면제.
 * requiredEnglishLevels: 해당 영어 레벨 학생에게만 규칙 적용. 목록에 없으면 자동 충족. ex. BasicEAS는 S4 전용.
 * applicableMajorRoles: SINGLE_PRIMARY·DUAL_PRIMARY·SECONDARY 중 이 규칙을 적용할 전공 역할.
 * 값이 없는 마이그레이션 전 규칙은 SINGLE_PRIMARY로 해석한다.
 */
@Component
public class RequiredCourseEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "REQUIRED_COURSE";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        String englishLevel = context.getTranscript().englishLevel();

        if (englishLevel != null) {
            if (config.exemptEnglishLevels() != null
                    && config.exemptEnglishLevels().contains(englishLevel)) {
                return new RuleResult(rule.ruleName(), true);
            }
            if (config.requiredEnglishLevels() != null
                    && !config.requiredEnglishLevels().contains(englishLevel)) {
                return new RuleResult(rule.ruleName(), true);
            }
        } else {
            // englishLevel이 null이면 레벨 지정 규칙(requiredEnglishLevels)은 자동 충족으로 처리
            if (config.requiredEnglishLevels() != null
                    && !config.requiredEnglishLevels().isEmpty()) {
                return new RuleResult(rule.ruleName(), true);
            }
        }

        boolean satisfied = context.getPassedCoursesForRule(rule.courseType()).stream()
                .anyMatch(record -> context.codeMatchesAny(record.courseCode(), config.courseCodes()));
        return new RuleResult(rule.ruleName(), satisfied);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(
            List<String> courseCodes, List<String> exemptEnglishLevels, List<String> requiredEnglishLevels) {}
}
