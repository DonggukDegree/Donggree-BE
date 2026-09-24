package com.donggree.transcript.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 개인정보 없이 PDF에서 확인한 성적·영역 공백 누락을 재현한다. */
class TranscriptParserAreaTest {
    private final TranscriptParser parser = new TranscriptParser();

    @ParameterizedTest
    @ValueSource(strings = {"A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "NP"})
    void 성적과_영역이_붙어도_등급과_영역을_분리한다(String grade) {
        var result = parser.parse("2022-1 1 전공 CSE0001 테스트과목 3 " + grade + "기초 2025 - 2 2학년 등필");
        assertThat(result.courses())
                .containsExactly(new ParsedCourse("2022-1", 1, "전공", "CSE0001", "테스트과목", 3, grade, "기초", false));
    }

    @ParameterizedTest
    @CsvSource({"전공,기초", "전필,전문", "복수1,전문", "복수2,기초", "공교,SW", "학기,4", "일교,1", "자선,전문"})
    void 유효_영역과_재수강_표시를_보존한다(String category, String area) {
        var joined = parser.parse("2023-1 2 " + category + " CSE0001 테스트과목 3 B+" + area + " R");
        var spaced = parser.parse("2023-1 2 " + category + " CSE0001 테스트과목 3 B+ " + area + " R");
        assertThat(joined).isEqualTo(spaced);
        assertThat(joined.courses().getFirst().area()).isEqualTo(area);
        assertThat(joined.courses().getFirst().retake()).isTrue();
    }

    @Test
    void 다음_과목의_정보를_이전_과목의_영역으로_가져오지_않는다() {
        var result = parser.parse(
                """
                2023-1 1 전공 CSE0001 영역없는과목 3 D+
                2023-2 1 전공 CSE0002 다음과목 3 A+기초
                """);
        assertThat(result.courses())
                .containsExactly(
                        new ParsedCourse("2023-1", 1, "전공", "CSE0001", "영역없는과목", 3, "D+", "", false),
                        new ParsedCourse("2023-2", 1, "전공", "CSE0002", "다음과목", 3, "A+", "기초", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025", "기초설명", "기초2025", "전문가", "미상"})
    void 뒤따르는_연도나_긴_단어를_유효_영역으로_오인하지_않는다(String suffix) {
        for (String category : new String[] {"전공", "학기"}) {
            var result = parser.parse("2023-1 1 " + category + " CSE0001 테스트과목 3 D+" + suffix);
            assertThat(result.courses().getFirst().grade()).isEqualTo("D+");
            assertThat(result.courses().getFirst().area()).isEmpty();
        }
    }

    @Test
    void 다른_이수구분의_영역은_허용하지_않는다() {
        var result = parser.parse("2023-1 1 전공 CSE0001 테스트과목 3 D+SW");
        assertThat(result.courses().getFirst().area()).isEmpty();
    }

    @Test
    void 영역이_없는_일반교양의_옆_열_등록년도를_숫자_영역으로_오인하지_않는다() {
        var result = parser.parse("2024-1 2 일교 EGC0001 테스트교양 1 P  2023 - 1 1학년 등필");
        assertThat(result.courses())
                .containsExactly(new ParsedCourse("2024-1", 2, "일교", "EGC0001", "테스트교양", 1, "P", "", false));
    }

    @Test
    void 기존_공백_줄바꿈_영역누락_재수강_형식을_유지한다() {
        for (String separator : new String[] {" ", "  ", "\t", "\n"}) {
            var result = parser.parse("2023-1 1 전공 CSE0001 테스트과목 3 D+" + separator + "기초 R");
            assertThat(result.courses().getFirst().area()).isEqualTo("기초");
            assertThat(result.courses().getFirst().retake()).isTrue();
        }
        var result = parser.parse("2023-1 1 전공 CSE0001 테스트과목 3 D+ R");
        assertThat(result.courses().getFirst().area()).isEmpty();
        assertThat(result.courses().getFirst().retake()).isTrue();
    }
}
