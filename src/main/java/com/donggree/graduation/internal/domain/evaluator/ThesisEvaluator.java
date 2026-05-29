package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 졸업논문(종합설계) 규칙 평가기.
 * ruleConfig:
 *   {
 *     "exemptStudentTypes": ["학석사연계과정"],
 *     "requiredCourseSets": [["종합설계1", "종합설계2"], ["종합설계1", "개별연구"]]
 *   }
 *
 * exemptStudentTypes에 해당하는 학생은 자동 충족 처리한다.
 * requiredCourseSets 중 하나의 세트를 구성하는 모든 과목을 이수하면 충족이다.
 */
@Component
public class ThesisEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "THESIS";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        String studentType = context.getTranscript().studentType();

        if (config.exemptStudentTypes() != null && config.exemptStudentTypes().contains(studentType)) {
            return new RuleResult(rule.ruleName(), true);
        }

        List<List<String>> courseSets = config.requiredCourseSets();
        boolean satisfied = courseSets != null
                && courseSets.stream().anyMatch(set -> set.stream().allMatch(context::hasPassedCourseByName));

        return new RuleResult(rule.ruleName(), satisfied);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(List<String> exemptStudentTypes, List<List<String>> requiredCourseSets) {}
}
