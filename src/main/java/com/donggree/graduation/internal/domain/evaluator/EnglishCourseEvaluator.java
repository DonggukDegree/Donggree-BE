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
 * 영어강의 이수 규칙 평가기.
 * ruleConfig:
 *   {"courseTypes": ["FIRST_MAJOR"], "minCount": 2, "applicableMajorRoles": ["SINGLE_PRIMARY"]}
 *   {"courseTypes": null, "minCount": 4, "applicableMajorRoles": ["DUAL_PRIMARY"]}
 *   {"courseTypes": ["FIRST_MAJOR"], "minCount": 4, "applicableMajorRoles": ["SECONDARY"]}
 *
 * 영어강의 대상(englishCourseTarget=false)이 아닌 학생은 자동으로 충족 처리한다.
 * 주전공은 PDF 이수 결과(completedEnglishResult=true)가 충족이면 기존처럼 계산을 생략한다.
 * 복수전공 학과의 별도 요건은 그 결과로 통과시키지 않고 실제 과목 수로 판정한다.
 * courseTypes가 null이면 전공 역할과 무관하게 전체 이수 과목에서 집계한다.
 * FIRST_MAJOR는 주전공 역할에서는 주전공, SECONDARY 역할에서는 복수1로 해석한다.
 * SECOND_MAJOR를 직접 선택해도 복수1만 집계한다. 여러 이수구분은 합산하며 학점이 아닌 과목 수다.
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
        if (context.getMajorRole().isPrimary() && Boolean.TRUE.equals(transcript.completedEnglishResult())) {
            return new RuleResult(rule.ruleName(), true);
        }

        // 영어강의는 선택한 이수구분으로 범위를 정한다. 다른 전공 규칙용 목록 제한을 적용하지 않는다.
        long count = transcript.courseRecords().stream()
                .filter(CourseRecordView::passed)
                .filter(cr -> isEnglishCourse(cr.courseName()))
                .filter(cr -> matchesCourseTypes(cr, config.courseTypes(), context))
                .count();

        return new RuleResult(rule.ruleName(), count >= config.minCount());
    }

    private boolean isEnglishCourse(String courseName) {
        return courseName != null && courseName.contains("<영어>");
    }

    private boolean matchesCourseTypes(CourseRecordView cr, List<String> courseTypes, EvaluationContext context) {
        if (courseTypes == null) return true;
        // 같은 학수번호라도 PDF의 복수1 표기가 있으면 제2전공으로 해석한다.
        CourseClassificationView cls = context.getClassification(cr);
        if (cls == null || cls.courseType() == null) return false;
        return courseTypes.stream().anyMatch(type -> {
            String effectiveType =
                    context.getMajorRole() == MajorRole.SECONDARY && "FIRST_MAJOR".equals(type) ? "SECOND_MAJOR" : type;
            if (CourseType.SECOND_MAJOR.name().equals(effectiveType)) {
                // 복수2를 복수1 학과의 전공 요건으로 인정하지 않는다.
                return "복수1".equals(cr.courseTypeName());
            }
            return cls.courseType().name().equals(effectiveType);
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(List<String> courseTypes, int minCount) {}
}
