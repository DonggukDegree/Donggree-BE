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
 * 영어강의 이수 규칙 평가기.
 * ruleConfig:
 *   {"courseTypes": ["FIRST_MAJOR"], "minCount": 2}   — 전공 영어강의 N개 이상
 *   {"courseTypes": null, "minCount": 4}               — 전체 영어강의 N개 이상
 *
 * 영어강의 대상(englishCourseTarget=false)이 아닌 학생은 자동으로 충족 처리한다.
 * PDF에서 이수 결과(completedEnglishResult=true)가 이미 충족으로 확인된 경우 계산을 생략한다.
 * 영어강의 여부는 과목명 내 "<영어>" 포함 여부로 식별한다.
 */
@Component
public class EnglishCourseEvaluator implements RuleEvaluator {

    @Override
    public String supportedTypeName() {
        return "ENGLISH_COURSE";
    }

    @Override
    public RuleResult evaluate(GraduationRuleView rule, EvaluationContext context) {
        Config config = RuleConfigParser.parse(rule.ruleConfig(), Config.class);
        var transcript = context.getTranscript();

        if (!transcript.englishCourseTarget()) {
            return new RuleResult(rule.ruleName(), true);
        }
        if (Boolean.TRUE.equals(transcript.completedEnglishResult())) {
            return new RuleResult(rule.ruleName(), true);
        }

        long count = context.getPassedCourses().stream()
                .filter(cr -> isEnglishCourse(cr.courseName()))
                .filter(cr -> matchesCourseTypes(cr, config.courseTypes(), context))
                .count();

        return new RuleResult(rule.ruleName(), count >= config.minCount());
    }

    private boolean isEnglishCourse(String courseName) {
        return courseName != null && courseName.contains("<영어>");
    }

    private boolean matchesCourseTypes(CourseRecordView cr, List<String> courseTypes,
                                       EvaluationContext context) {
        if (courseTypes == null) return true;
        CourseClassificationView cls = context.getClassification(cr.courseCode());
        if (cls == null) return false;
        return courseTypes.contains(cls.courseType().name());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(List<String> courseTypes, int minCount) {}
}
