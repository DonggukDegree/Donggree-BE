package com.donggree.graduation.internal.domain.evaluator;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleEvaluator;
import com.donggree.graduation.internal.domain.RuleResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 졸업논문 규칙 평가기.
 * 졸업 판정 방식이 학과마다 다르므로 requiredCourseSets 유무로 판정 경로를 나눈다.
 *
 * ruleConfig:
 *   // 지정 과목 이수로 판정 (ex. 컴퓨터·AI학부 종합설계)
 *   {
 *     "exemptStudentTypes": ["학석사연계과정"],
 *     "requiredCourseSets": [
 *       [["CSE4066","CSC4018"], ["CSE4067","CSC4019"]],
 *       [["CSE4066","CSC4018"], ["DAI*"]]
 *     ]
 *   }
 *
 *   // 일부 과목만 면제 (ex. 컴퓨터·AI학부 심화과정 — 학석사연계과정은 종합설계2·개별연구만 면제)
 *   {
 *     "exemptStudentTypes": ["학석사연계과정"],
 *     "exemptCourseCodes": ["CSE4067","CSC4019","DAI*"],
 *     "requiredCourseSets": [
 *       [["CSE4066","CSC4018"], ["CSE4067","CSC4019"]],
 *       [["CSE4066","CSC4018"], ["DAI*"]]
 *     ]
 *   }
 *
 *   // 성적표의 졸업논문심사 결과로 판정 (대부분의 학과)
 *   {}
 *
 * 판정 순서:
 *   1. exemptStudentTypes에 해당하는 학생 중 exemptCourseCodes가 없으면 — 규칙 전체 면제(자동 충족)
 *   2. requiredCourseSets가 없으면 — 성적표의 졸업논문심사가 합격이면 충족
 *   3. requiredCourseSets가 있으면 — 그중 한 세트에서 각 과목 그룹마다 하나 이상 이수하면 충족
 *      (동일유사 교과목은 같은 그룹 배열에 여러 코드로 표현한다)
 *
 * 부분 면제(exemptCourseCodes):
 *   면제 대상 학생에게 <b>그 과목들을 이수한 것으로 간주</b>하고 나머지는 평소대로 판정한다.
 *   "학석사연계과정이면 종합설계2 또는 개별연구만 면제하고 종합설계1은 그대로 요구" 같은 경우에 쓴다.
 *   그룹 충족 판정이 "그룹 안의 코드 중 하나"이므로, 면제 코드가 그룹에 하나라도 들어 있으면 그 그룹은 충족된다.
 *   위 예시에서 종합설계2 그룹과 개별연구 그룹은 면제로 충족되고, 종합설계1 그룹만 실제 이수가 필요해진다.
 *   requiredCourseSets가 없는(졸업논문심사 기반) 규칙에서는 의미가 없다.
 *
 * 학과 구분은 이 규칙이 아니라 requirement_set(department_id)이 담당하므로
 * 규칙 자체에 적용 학과를 두지 않는다.
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
        boolean exempt = config.exemptStudentTypes() != null
                && config.exemptStudentTypes().contains(studentType);

        // 면제 대상이고 면제 과목이 따로 지정되지 않았으면 규칙 전체를 면제한다.
        if (exempt && isEmpty(config.exemptCourseCodes())) {
            return new RuleResult(rule.ruleName(), true);
        }

        List<List<List<String>>> courseSets = config.requiredCourseSets();
        if (courseSets == null || courseSets.isEmpty()) {
            return new RuleResult(rule.ruleName(), context.getTranscript().thesisStatus());
        }

        // 부분 면제일 때만 면제 과목 목록을 적용한다. 면제 대상이 아닌 학생에겐 영향이 없다.
        List<String> exemptedCodes = exempt ? config.exemptCourseCodes() : null;
        boolean satisfied = courseSets.stream()
                .anyMatch(set -> set.stream().allMatch(codes -> isGroupSatisfied(codes, exemptedCodes, context)));

        return new RuleResult(rule.ruleName(), satisfied);
    }

    /**
     * 과목 그룹 하나가 충족됐는지 판단한다.
     * 그룹 안의 코드 중 하나라도 이수했거나, 면제 과목으로 지정돼 이수한 것으로 간주되면 충족이다.
     */
    private boolean isGroupSatisfied(List<String> codes, List<String> exemptedCodes, EvaluationContext context) {
        if (!isEmpty(exemptedCodes) && codes != null && codes.stream().anyMatch(exemptedCodes::contains)) {
            return true;
        }
        return context.hasPassedAnyCourseByCode(codes);
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(
            List<String> exemptStudentTypes,
            List<String> exemptCourseCodes,
            List<List<List<String>>> requiredCourseSets) {}
}
