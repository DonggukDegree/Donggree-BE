package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MinCreditsEvaluatorTest extends EvaluatorTestSupport {

    private final MinCreditsEvaluator evaluator = new MinCreditsEvaluator();

    // --- subCategory 선택자 + minCount (구 SCIENCE_EXPERIMENT: 실험 교과목 중 1과목 필수 선택) ---

    @Test
    void 실험_과목을_1개_이상_이수하면_충족이다() {
        var records = List.of(passed("PRI4002", "일반물리학및실험1", 3, "2023-1"));
        var cls = Map.of("PRI4002", classification(CourseType.ACADEMIC_FOUNDATION, "과학", "실험", "물리학"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(experimentRule(), ctx).satisfied()).isTrue();
    }

    @Test
    void 실험_과목이_없으면_미충족이다() {
        var records = List.of(passed("PRI4006", "물리학개론", 3, "2023-1"));
        var cls = Map.of("PRI4006", classification(CourseType.ACADEMIC_FOUNDATION, "과학", "개론", "물리학"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(experimentRule(), ctx).satisfied()).isFalse();
    }

    @Test
    void 실험_과목_이수_실패는_카운트되지_않는다() {
        var records = List.of(failed("PRI4002", "일반물리학및실험1", 3, "2023-1"));
        var cls = Map.of("PRI4002", classification(CourseType.ACADEMIC_FOUNDATION, "과학", "실험", "물리학"));
        EvaluationContext ctx = context(transcript(0, 0.0, records), cls);

        assertThat(evaluator.evaluate(experimentRule(), ctx).satisfied()).isFalse();
    }

    @Test
    void 같은_소분류라도_다른_이수구분이면_대상이_아니다() {
        var records = List.of(passed("RGC9999", "교양실험과목", 3, "2023-1"));
        var cls = Map.of("RGC9999", classification(CourseType.COMMON_GENERAL, "문화", "실험", null));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(experimentRule(), ctx).satisfied()).isFalse();
    }

    // --- areaName 선택자 + minCredits (여러 영역을 합쳐 최소 학점을 채우는 규칙) ---

    @Test
    void 여러_영역의_학점을_합쳐_최소_학점을_채우면_충족이다() {
        var records = List.of(passed("RGC1002", "글로벌시민의식", 1, "2023-1"), passed("RGC1075", "동남아지역연구", 1, "2023-2"));
        var cls = Map.of(
                "RGC1002", classification(CourseType.COMMON_GENERAL, "21세기시민"),
                "RGC1075", classification(CourseType.COMMON_GENERAL, "지역연구"));
        EvaluationContext ctx = context(transcript(2, 4.0, records), cls);

        assertThat(evaluator.evaluate(multiAreaRule(), ctx).satisfied()).isTrue();
    }

    @Test
    void 대상_영역_학점이_모자라면_미충족이다() {
        var records = List.of(passed("RGC1002", "글로벌시민의식", 1, "2023-1"));
        var cls = Map.of("RGC1002", classification(CourseType.COMMON_GENERAL, "21세기시민"));
        EvaluationContext ctx = context(transcript(1, 4.0, records), cls);

        assertThat(evaluator.evaluate(multiAreaRule(), ctx).satisfied()).isFalse();
    }

    @Test
    void 대상_영역이_아닌_과목의_학점은_합산되지_않는다() {
        var records = List.of(passed("RGC0005", "글쓰기와의사소통", 3, "2023-1"));
        var cls = Map.of("RGC0005", classification(CourseType.COMMON_GENERAL, "글쓰기"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(multiAreaRule(), ctx).satisfied()).isFalse();
    }

    // --- 이수구분 전체 최소학점 (구 MIN_AREA_CREDITS, areaNames=null) ---

    @Test
    void 이수구분_전체_학점이_최소_이상이면_충족이다() {
        var records = List.of(
                passed("RGC0003", "불교와인간", 2, "2023-1"),
                passed("RGC1080", "EAS1", 3, "2023-2"),
                passed("RGC1081", "EAS2", 3, "2024-1"));
        var cls = Map.of(
                "RGC0003", classification(CourseType.COMMON_GENERAL, "자아성찰"),
                "RGC1080", classification(CourseType.COMMON_GENERAL, "영어"),
                "RGC1081", classification(CourseType.COMMON_GENERAL, "영어"));
        EvaluationContext ctx = context(transcript(8, 4.0, records), cls);
        var rule = rule("{\"courseType\": \"COMMON_GENERAL\", \"areaNames\": null, \"minCredits\": 8}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    @Test
    void 이수구분_전체_학점이_부족하면_미충족이다() {
        var records = List.of(passed("RGC0003", "불교와인간", 2, "2023-1"));
        var cls = Map.of("RGC0003", classification(CourseType.COMMON_GENERAL, "자아성찰"));
        EvaluationContext ctx = context(transcript(2, 4.0, records), cls);
        var rule = rule("{\"courseType\": \"COMMON_GENERAL\", \"areaNames\": null, \"minCredits\": 17}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    @Test
    void 이수_실패_과목은_학점에_포함되지_않는다() {
        var records = List.of(failed("CSC2007", "자료구조", 3, "2023-1"));
        var cls = Map.of("CSC2007", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(0, 0.0, records), cls);
        var rule = rule("{\"courseType\": \"FIRST_MAJOR\", \"areaNames\": null, \"minCredits\": 3}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    // --- pdfAreaName 선택자 (전공기초/전공전문 — course_classification 미등록 과목) ---

    /**
     * 전공 과목은 course_classification에 등록돼 있지 않아 전공기초/전공전문 구분이 PDF에만 있다.
     * courseType은 PDF 이수구분("전공")으로 추론된 값을 쓰고, 영역은 PDF 원문("기초"/"전문")으로 고른다.
     */
    @Test
    void 전공전문_학점이_최소_이상이면_충족이다() {
        var records = List.of(
                majorCourse("CSE4029", "알고리즘", 3, "전문"),
                majorCourse("CSE3012", "운영체제", 3, "전문"),
                majorCourse("CSE1001", "컴퓨터과학개론", 3, "기초"));
        var cls = Map.of(
                "CSE4029", classification(CourseType.FIRST_MAJOR),
                "CSE3012", classification(CourseType.FIRST_MAJOR),
                "CSE1001", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(9, 4.0, records), cls);

        assertThat(evaluator.evaluate(majorAdvancedRule(6), ctx).satisfied()).isTrue();
    }

    @Test
    void 전공기초_학점은_전공전문_규칙에_합산되지_않는다() {
        var records = List.of(majorCourse("CSE1001", "컴퓨터과학개론", 3, "기초"), majorCourse("CSE1002", "어드벤처디자인", 3, "기초"));
        var cls = Map.of(
                "CSE1001", classification(CourseType.FIRST_MAJOR),
                "CSE1002", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(6, 4.0, records), cls);

        assertThat(evaluator.evaluate(majorAdvancedRule(6), ctx).satisfied()).isFalse();
    }

    // --- pdfCourseTypeName 선택자 (전필 — PDF 이수구분 원문) ---

    /**
     * 전공필수 여부는 PDF의 이수구분 칸("전필")에만 있고 영역 칸에는 "기초"/"전문"이 들어간다.
     * 따라서 전필 학점은 pdfAreaNames가 아니라 pdfCourseTypeNames로 골라야 한다.
     */
    @Test
    void 전필_과목의_학점만_합산해_최소_학점을_판정한다() {
        var records = List.of(
                majorRequiredCourse("CSC2007", "자료구조", 3, "전문"),
                majorRequiredCourse("CSC2011", "컴퓨터구성", 3, "전문"),
                majorCourse("CSE4029", "알고리즘", 3, "전문"));
        var cls = Map.of(
                "CSC2007", classification(CourseType.FIRST_MAJOR),
                "CSC2011", classification(CourseType.FIRST_MAJOR),
                "CSE4029", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(9, 4.0, records), cls);

        assertThat(evaluator.evaluate(majorRequiredRule(6), ctx).satisfied()).isTrue();
        assertThat(evaluator.evaluate(majorRequiredRule(9), ctx).satisfied()).isFalse();
    }

    @Test
    void 전필이_아닌_전공_과목은_전필_규칙에_합산되지_않는다() {
        var records = List.of(majorCourse("CSE4029", "알고리즘", 3, "전문"), majorCourse("CSE3012", "운영체제", 3, "전문"));
        var cls = Map.of(
                "CSE4029", classification(CourseType.FIRST_MAJOR),
                "CSE3012", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(6, 4.0, records), cls);

        assertThat(evaluator.evaluate(majorRequiredRule(3), ctx).satisfied()).isFalse();
    }

    @Test
    void 전필_과목도_영역_원문으로_고르면_전공전문_규칙에_합산된다() {
        var records = List.of(majorRequiredCourse("CSC2007", "자료구조", 3, "전문"));
        var cls = Map.of("CSC2007", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(majorAdvancedRule(3), ctx).satisfied()).isTrue();
    }

    // --- courseCode 선택자 ---

    @Test
    void 학수번호_prefix_패턴으로도_선택할_수_있다() {
        var records = List.of(passed("DAI4001", "개별연구", 3, "2023-1"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), Map.of());
        var rule = rule("{\"courseCodes\": [\"DAI*\"], \"minCount\": 1}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    // --- 선택자 조합 (AND) ---

    /**
     * 선택자를 여러 개 쓰면 모두 만족하는 과목만 대상이다.
     * "전필 중 전공전문 영역" 처럼 교집합으로 좁히는 요건을 표현한다.
     */
    @Test
    void 선택자를_여러_개_쓰면_모두_만족하는_과목만_대상이다() {
        var records = List.of(
                majorRequiredCourse("CSC2007", "자료구조", 3, "전문"), // 전필 + 전문 → 대상
                majorRequiredCourse("CSE1001", "컴퓨터과학개론", 3, "기초"), // 전필이지만 기초 → 제외
                majorCourse("CSE4029", "알고리즘", 3, "전문")); // 전문이지만 전필 아님 → 제외
        var cls = Map.of(
                "CSC2007", classification(CourseType.FIRST_MAJOR),
                "CSE1001", classification(CourseType.FIRST_MAJOR),
                "CSE4029", classification(CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(9, 4.0, records), cls);
        var rule = rule(
                "{\"courseType\":\"FIRST_MAJOR\",\"pdfCourseTypeNames\":[\"전필\"],\"pdfAreaNames\":[\"전문\"],\"minCredits\":3}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();

        var stricter = rule(
                "{\"courseType\":\"FIRST_MAJOR\",\"pdfCourseTypeNames\":[\"전필\"],\"pdfAreaNames\":[\"전문\"],\"minCredits\":6}");
        assertThat(evaluator.evaluate(stricter, ctx).satisfied()).isFalse();
    }

    @Test
    void 영역과_소분류를_함께_쓰면_둘_다_일치해야_대상이다() {
        var records = List.of(
                passed("PRI4002", "일반물리학및실험1", 3, "2023-1"), // 과학 + 실험 → 대상
                passed("PRI4030", "공학수학실험", 3, "2023-2")); // 실험이지만 수학 영역 → 제외
        var cls = Map.of(
                "PRI4002", classification(CourseType.ACADEMIC_FOUNDATION, "과학", "실험", "물리학"),
                "PRI4030", classification(CourseType.ACADEMIC_FOUNDATION, "수학", "실험", null));
        EvaluationContext ctx = context(transcript(6, 4.0, records), cls);
        var rule = rule(
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"과학\"],\"subCategories\":[\"실험\"],\"minCount\":1,\"minCredits\":3}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();

        var needsTwo = rule(
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"과학\"],\"subCategories\":[\"실험\"],\"minCount\":2}");
        assertThat(evaluator.evaluate(needsTwo, ctx).satisfied()).isFalse();
    }

    @Test
    void 분류_정보가_없는_과목은_분류_기반_선택자에_걸리지_않는다() {
        var records = List.of(majorRequiredCourse("CSC9999", "미등록전공과목", 3, "전문"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), Map.of());
        var rule = rule("{\"pdfCourseTypeNames\":[\"전필\"],\"areaNames\":[\"전공전문\"],\"minCredits\":3}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    // --- 선택자·임계값 조합 ---

    @Test
    void 선택자가_비면_이수구분_전체가_대상이다() {
        var records = List.of(passed("RGC0005", "글쓰기와의사소통", 3, "2023-1"));
        var cls = Map.of("RGC0005", classification(CourseType.COMMON_GENERAL, "글쓰기"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);
        var rule = rule("{\"courseType\": \"COMMON_GENERAL\", \"minCredits\": 2}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    @Test
    void 학점과_과목수를_함께_지정하면_둘_다_충족해야_한다() {
        var records = List.of(passed("RGC1002", "글로벌시민의식", 3, "2023-1"));
        var cls = Map.of("RGC1002", classification(CourseType.COMMON_GENERAL, "21세기시민"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);
        var rule = rule("{\"areaNames\": [\"21세기시민\"], \"minCredits\": 2, \"minCount\": 2}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    @Test
    void 임계값이_없으면_설정_오류다() {
        EvaluationContext ctx = context(transcript(0, 0.0, List.of()), Map.of());
        var rule = rule("{\"areaNames\": [\"21세기시민\"]}");

        assertThatThrownBy(() -> evaluator.evaluate(rule, ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minCredits 또는 minCount");
    }

    private GraduationRuleView experimentRule() {
        return new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.ACADEMIC_FOUNDATION,
                "실험 교과목 중 1과목을 필수 선택해야 합니다.",
                "{\"courseType\": \"ACADEMIC_FOUNDATION\", \"subCategories\": [\"실험\"], \"minCount\": 1}");
    }

    private GraduationRuleView multiAreaRule() {
        return new GraduationRuleView(
                2L,
                "MIN_CREDITS",
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\": \"COMMON_GENERAL\", \"areaNames\": [\"21세기시민\", \"지역연구\", \"미래위험사회와안전\"], \"minCredits\": 2}");
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(3L, "MIN_CREDITS", CourseType.COMMON_GENERAL, "최소 이수량 규칙", config);
    }

    private GraduationRuleView majorAdvancedRule(int minCredits) {
        return new GraduationRuleView(
                4L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "전공전문 과목을 " + minCredits + "학점 이상 이수해야 합니다.",
                "{\"courseType\":\"FIRST_MAJOR\",\"pdfAreaNames\":[\"전문\"],\"minCredits\":" + minCredits + "}");
    }

    private GraduationRuleView majorRequiredRule(int minCredits) {
        return new GraduationRuleView(
                5L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "전공필수 과목을 " + minCredits + "학점 이상 이수해야 합니다.",
                "{\"courseType\":\"FIRST_MAJOR\",\"pdfCourseTypeNames\":[\"전필\"],\"minCredits\":" + minCredits + "}");
    }

    /** PDF 이수구분 "전공" + 영역 원문(기초/전문)을 가진 수강 이력. */
    private static CourseRecordView majorCourse(String code, String name, int credits, String pdfAreaName) {
        return new CourseRecordView("2024-1", code, "전공", pdfAreaName, name, credits, true, false);
    }

    /** PDF 이수구분 "전필"(전공필수) + 영역 원문(기초/전문)을 가진 수강 이력. */
    private static CourseRecordView majorRequiredCourse(String code, String name, int credits, String pdfAreaName) {
        return new CourseRecordView("2024-1", code, "전필", pdfAreaName, name, credits, true, false);
    }
}
