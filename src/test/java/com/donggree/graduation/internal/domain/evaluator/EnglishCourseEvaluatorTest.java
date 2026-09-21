package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnglishCourseEvaluatorTest extends EvaluatorTestSupport {

    private final EnglishCourseEvaluator evaluator = new EnglishCourseEvaluator();

    @Test
    void 영어강의_대상자가_아니면_자동_충족이다() {
        var t = transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void PDF_이수_결과가_true이면_자동_충족이다() {
        var t = transcriptWith(0, 4.0, true, true, false, "단일", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 전공_영어강의를_충분히_이수하면_충족이다() {
        var records = List.of(passed("MAJOR001", "<영어>알고리즘", 3, "2023-1"), passed("MAJOR002", "<영어>자료구조", 3, "2023-2"));
        var cls = Map.of(
                "MAJOR001", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null),
                "MAJOR002", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null));
        var t = transcriptWith(6, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 전공_영어강의가_부족하면_미충족이다() {
        var records = List.of(passed("MAJOR001", "<영어>알고리즘", 3, "2023-1"));
        var cls = Map.of("MAJOR001", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null));
        var t = transcriptWith(3, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void courseType_null이면_전체_영어강의를_합산한다() {
        var records = List.of(
                passed("GEN001", "<영어>교양영어", 2, "2023-1"),
                passed("MAJOR001", "<영어>알고리즘", 3, "2023-2"),
                passed("GEN002", "<영어>글쓰기", 2, "2024-1"),
                passed("MAJOR002", "<영어>자료구조", 3, "2024-2"));
        var cls = Map.of(
                "GEN001", new CourseClassificationView(CourseType.COMMON_GENERAL, null, null, null),
                "MAJOR001", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null),
                "GEN002", new CourseClassificationView(CourseType.LIBERAL_ARTS, null, null, null),
                "MAJOR002", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null));
        var t = transcriptWith(10, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\": null, \"minCount\": 4}"), ctx)
                        .satisfied())
                .isTrue();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "ENGLISH_COURSE", null, "영어강의 이수 규칙", config);
    }

    @ParameterizedTest
    @EnumSource(MajorRole.class)
    void 이수구분_미지정이면_역할과_무관하게_복수전공과_분류_미등록_과목까지_센다(MajorRole role) {
        var ctx = roleContext(role, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":null,\"minCount\":7}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(evaluator.evaluate(rule("{\"minCount\":8}"), ctx).satisfied())
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = MajorRole.class,
            names = {"SINGLE_PRIMARY", "DUAL_PRIMARY"})
    void 주전공_제1전공은_전공과_전필만_세고_같은_분류의_복수1을_섞지_않는다(MajorRole role) {
        var ctx = roleContext(role, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":3}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 복수전공_제1전공은_복수1만_세고_복수2와_미이수와_비영어강의를_제외한다() {
        var ctx = roleContext(MajorRole.SECONDARY, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":3}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(MajorRole.class)
    void 제2전공_직접_선택도_복수1만_집계한다(MajorRole role) {
        var ctx = roleContext(role, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"SECOND_MAJOR\"],\"minCount\":2}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"SECOND_MAJOR\"],\"minCount\":3}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 주전공에서_제1전공과_제2전공을_선택하면_영어강의_과목_수를_합산한다() {
        var ctx = roleContext(MajorRole.DUAL_PRIMARY, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\",\"SECOND_MAJOR\"],\"minCount\":4}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\",\"SECOND_MAJOR\"],\"minCount\":5}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 복수전공에서_제1전공과_제2전공을_선택해도_같은_복수1을_두번_세지_않는다() {
        var ctx = roleContext(MajorRole.SECONDARY, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\",\"SECOND_MAJOR\"],\"minCount\":3}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 복수전공_추가_요건은_PDF_전체_충족_표시만으로_자동_충족되지_않는다() {
        var ctx = roleContext(MajorRole.SECONDARY, true, true);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":3}"), ctx)
                        .satisfied())
                .isFalse();
        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 복수전공자의_주전공은_기존_PDF_전체_충족_처리를_유지한다() {
        var ctx = roleContext(MajorRole.DUAL_PRIMARY, true, true);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":99}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 복수전공_규칙도_영어강의_비대상자_면제를_유지한다() {
        var ctx = roleContext(MajorRole.SECONDARY, false, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":99}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 영어강의_범위_수정이_다른_규칙용_수강_목록을_바꾸지_않는다() {
        var ctx = roleContext(MajorRole.DUAL_PRIMARY, true, null);

        assertThat(evaluator
                        .evaluate(rule("{\"courseTypes\":null,\"minCount\":7}"), ctx)
                        .satisfied())
                .isTrue();
        assertThat(ctx.getPassedCourses())
                .noneMatch(record -> "복수1".equals(record.courseTypeName()) || "복수2".equals(record.courseTypeName()));
    }

    private EvaluationContext roleContext(MajorRole role, boolean target, Boolean completed) {
        var records = List.of(
                englishRecord("P1", "전공", true, true),
                englishRecord("P2", "전필", true, true),
                englishRecord("B1", "복수1", true, true),
                englishRecord("B2", "복수1", true, true),
                englishRecord("B3", "복수1", false, true),
                englishRecord("B4", "복수1", true, false),
                englishRecord("X1", "복수2", true, true),
                englishRecord("G1", "공교", true, true),
                englishRecord("UNKNOWN", null, true, true));
        // 복수1/복수2의 DB 분류가 FIRST_MAJOR여도 PDF 원문을 우선해야 한다.
        var cls = Map.of(
                "P1", classification(CourseType.FIRST_MAJOR),
                "P2", classification(CourseType.FIRST_MAJOR),
                "B1", classification(CourseType.FIRST_MAJOR),
                "B2", classification(CourseType.FIRST_MAJOR),
                "B3", classification(CourseType.FIRST_MAJOR),
                "B4", classification(CourseType.FIRST_MAJOR),
                "X1", classification(CourseType.FIRST_MAJOR),
                "G1", classification(CourseType.COMMON_GENERAL));
        var transcript = transcriptWith(27, 4.0, target, completed, false, "학사과정", "S1", records);
        return role == MajorRole.SECONDARY
                ? EvaluationContext.secondary(transcript, cls, "복수1")
                : EvaluationContext.primary(transcript, cls, role);
    }

    private CourseRecordView englishRecord(String code, String pdfType, boolean passed, boolean english) {
        return new CourseRecordView("2023-1", code, pdfType, null, (english ? "<영어>" : "") + code, 3, passed, false);
    }
}
