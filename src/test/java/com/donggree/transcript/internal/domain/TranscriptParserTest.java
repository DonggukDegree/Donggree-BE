package com.donggree.transcript.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TranscriptParserTest {

    private final TranscriptParser parser = new TranscriptParser();

    // ====== 기본 동작 테스트 ======

    @Test
    @DisplayName("null 텍스트 입력 시 예외가 발생한다")
    void parse_null_text_throws_exception() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pdfText");
    }

    @Test
    @DisplayName("빈 텍스트 입력 시 빈 결과를 반환한다")
    void parse_empty_text_returns_empty_result() {
        ParsedTranscriptData result = parser.parse("");

        assertThat(result.courses()).isEmpty();
    }

    // ====== 텍스트 픽스처 기반 파싱 테스트 (CI에서 항상 실행) ======

    @Nested
    @DisplayName("텍스트 픽스처 기반 파싱")
    class TextFixtureParsingTest {

        private final ParsedTranscriptData result = parseFixture();

        @Test
        @DisplayName("메타 정보에 필수 키가 포함된다")
        void meta_contains_required_keys() {
            Map<String, String> meta = result.meta();

            assertThat(meta).containsKey("학과");
            assertThat(meta).containsKey("학번");
            assertThat(meta).containsKey("학적상태");
            assertThat(meta).containsKey("총취득학점");
            assertThat(meta).containsKey("평점평균");
            assertThat(meta).containsKey("이수학기");
        }

        @Test
        @DisplayName("학과에서 학년이 분리된다")
        void department_and_year_are_separated() {
            assertThat(result.meta().get("학과")).isEqualTo("테스트학부");
            assertThat(result.meta().get("학년")).isEqualTo("4");
        }

        @Test
        @DisplayName("메타 총취득학점과 파싱된 과목 학점 합계가 일치한다")
        void total_credits_matches_sum() {
            int metaTotal = Integer.parseInt(result.meta().get("총취득학점"));
            int parsedTotal =
                    result.courses().stream().mapToInt(ParsedCourse::credits).sum();

            assertThat(parsedTotal).isEqualTo(metaTotal);
        }

        @Test
        @DisplayName("모든 이수구분이 파싱된다")
        void all_categories_are_parsed() {
            Set<String> categories = Set.of("공교", "전공", "전필", "일교", "학기", "자선");
            Set<String> parsedCategories = new java.util.HashSet<>();
            result.courses().forEach(c -> parsedCategories.add(c.category()));

            assertThat(parsedCategories).containsAll(categories);
        }

        @Test
        @DisplayName("과목 수가 올바르다")
        void course_count_is_correct() {
            assertThat(result.courses()).hasSize(12);
        }

        @Test
        @DisplayName("과목 필드가 올바르게 파싱된다")
        void course_fields_are_parsed_correctly() {
            ParsedCourse first = result.courses().get(0);

            assertThat(first.semester()).isEqualTo("2023-1");
            assertThat(first.year()).isEqualTo(1);
            assertThat(first.category()).isEqualTo("공교");
            assertThat(first.courseCode()).isEqualTo("RGC0001");
            assertThat(first.courseName()).isEqualTo("자아와명상1");
            assertThat(first.credits()).isEqualTo(1);
            assertThat(first.grade()).isEqualTo("P");
            assertThat(first.area()).isEqualTo("자아");
            assertThat(first.retake()).isFalse();
        }

        @Test
        @DisplayName("재수강 과목이 올바르게 파싱된다")
        void retake_course_is_parsed() {
            List<ParsedCourse> retakes =
                    result.courses().stream().filter(ParsedCourse::retake).toList();

            assertThat(retakes).hasSize(1);
            assertThat(retakes.get(0).courseCode()).isEqualTo("CSE3001");
        }

        @Test
        @DisplayName("영역이 올바르게 파싱된다")
        void areas_are_parsed_correctly() {
            Map<String, String> expectedAreas = Map.of(
                    "RGC0001", "자아",
                    "RGC0002", "영어",
                    "RGC1080", "SW",
                    "CSE1001", "기초",
                    "CSE2001", "전문",
                    "DBA2001", "1",
                    "PRI4001", "4");

            for (ParsedCourse course : result.courses()) {
                if (expectedAreas.containsKey(course.courseCode())) {
                    assertThat(course.area())
                            .as("과목 %s의 영역", course.courseCode())
                            .isEqualTo(expectedAreas.get(course.courseCode()));
                }
            }
        }

        @Test
        @DisplayName("성적이 유효한 값이다")
        void grades_are_valid() {
            Set<String> validGrades = Set.of("A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "NP");

            for (ParsedCourse course : result.courses()) {
                assertThat(validGrades)
                        .as("과목 %s의 성적 %s", course.courseCode(), course.grade())
                        .contains(course.grade());
            }
        }

        @Test
        @DisplayName("학기 형식이 올바르다")
        void semester_format_is_valid() {
            for (ParsedCourse course : result.courses()) {
                assertThat(course.semester()).matches("\\d{4}-(1|2|여름|겨울)");
            }
        }

        @Test
        @DisplayName("이수학기가 올바르게 계산된다")
        void completed_semesters_are_calculated() {
            assertThat(result.meta().get("이수학기")).isEqualTo("4");
        }

        @Test
        @DisplayName("하단 요약 정보가 올바르게 파싱된다")
        void summary_fields_are_parsed() {
            Map<String, String> meta = result.meta();

            assertThat(meta.get("평점평균")).isEqualTo("3.85");
            assertThat(meta.get("제1전공총학점")).isEqualTo("15");
            assertThat(meta.get("공통교양총학점")).isEqualTo("7");
            assertThat(meta.get("영어강의이수결과")).isEqualTo("PASS");
            assertThat(meta.get("졸업논문심사")).isEqualTo("합격");
            assertThat(meta.get("주전공")).isEqualTo("테스트학부");
        }

        @Test
        @DisplayName("영어패스제: 대상 학생의 PASS/FAIL이 파싱된다")
        void english_pass_result_is_parsed() {
            assertThat(parser.parseMeta("영어패스제 대상 FAIL\n").get("영어패스제결과")).isEqualTo("FAIL");
            assertThat(parser.parseMeta("영어패스제 대상 PASS\n").get("영어패스제결과")).isEqualTo("PASS");
        }

        @Test
        @DisplayName("영어패스제: 뒤에 PASS/FAIL이 없으면 null이다 (미대상 학생)")
        void english_pass_result_is_null_when_no_verdict() {
            // 미대상 학생은 PDF에 판정값이 찍히지 않는다. 이때 FAIL로 잘못 저장되면
            // 해당 없는 학생에게 유의사항 문구가 뜨므로 반드시 null이어야 한다.
            assertThat(parser.parseMeta("영어패스제 미대상\n").get("영어패스제결과")).isNull();
        }

        @Test
        @DisplayName("영어패스제: 다음 줄의 PASS/FAIL을 끌어오지 않는다")
        void english_pass_result_does_not_cross_lines() {
            // 정규식에 DOTALL이 없어 '.'이 줄바꿈을 넘지 못한다.
            // 넘게 되면 바로 아래 줄의 영어강의이수 결과를 영어패스제 결과로 잘못 읽는다.
            String text = "영어패스제 미대상\n영어강의이수 : PASS\n";
            assertThat(parser.parseMeta(text).get("영어패스제결과")).isNull();
        }

        private ParsedTranscriptData parseFixture() {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("transcript-fixture.txt")) {
                String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return parser.parse(text);
            } catch (IOException e) {
                throw new RuntimeException("텍스트 픽스처 로딩 실패", e);
            }
        }
    }

    @Nested
    @DisplayName("복수전공 텍스트 픽스처 기반 파싱")
    class DualMajorTextFixtureParsingTest {

        private final ParsedTranscriptData result = parseDualMajorFixture();

        @Test
        @DisplayName("복수전공 학과명과 기준 학기를 분리한다")
        void dual_major_department_and_reference_semester_are_separated() {
            assertThat(result.meta().get("복수1")).isEqualTo("테스트복수전공");
            assertThat(result.meta().get("복수1기준학기")).isEqualTo("2026-2");
            assertThat(result.meta().get("복수2")).isNull();
            assertThat(result.meta().get("복수2기준학기")).isNull();
        }

        @Test
        @DisplayName("복수전공 과목과 기초·전문 영역을 파싱한다")
        void dual_major_courses_and_areas_are_parsed() {
            List<ParsedCourse> dualMajorCourses = result.courses().stream()
                    .filter(course -> "복수1".equals(course.category()))
                    .toList();

            assertThat(dualMajorCourses).hasSize(3);
            assertThat(dualMajorCourses)
                    .extracting(ParsedCourse::courseCode)
                    .containsExactly("CSC4016", "ENE2002", "ENE2003");
            assertThat(dualMajorCourses).extracting(ParsedCourse::area).containsExactly("전문", "기초", "기초");
            assertThat(dualMajorCourses).extracting(ParsedCourse::credits).containsOnly(3);
        }

        @Test
        @DisplayName("공통 학기의 과목을 파싱한다")
        void common_semester_course_is_parsed() {
            assertThat(result.courses()).anySatisfy(course -> {
                assertThat(course.semester()).isEqualTo("2023-공통");
                assertThat(course.courseCode()).isEqualTo("RGC1051");
                assertThat(course.credits()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("복수전공 요약 정보를 파싱한다")
        void dual_major_summary_is_parsed() {
            Map<String, String> meta = result.meta();

            assertThat(meta.get("복수1총학점")).isEqualTo("9");
            assertThat(meta.get("복수1기초학점")).isEqualTo("6");
            assertThat(meta.get("복수1전문학점")).isEqualTo("3");
            assertThat(meta.get("복수1평점")).isEqualTo("4.17");
            assertThat(meta.get("복수1졸업논문심사")).isEqualTo("미판정");
        }

        @Test
        @DisplayName("총취득학점과 파싱된 과목 학점 합계가 일치한다")
        void total_credits_matches_sum() {
            int parsedTotal =
                    result.courses().stream().mapToInt(ParsedCourse::credits).sum();

            assertThat(result.courses()).hasSize(5);
            assertThat(parsedTotal).isEqualTo(Integer.parseInt(result.meta().get("총취득학점")));
        }

        private ParsedTranscriptData parseDualMajorFixture() {
            try (InputStream is =
                    getClass().getClassLoader().getResourceAsStream("transcript-dual-major-fixture.txt")) {
                String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return parser.parse(text);
            } catch (IOException e) {
                throw new RuntimeException("복수전공 텍스트 픽스처 로딩 실패", e);
            }
        }
    }

    // ====== PDF 파싱 테스트 (로컬 전용, PDF 파일이 있을 때만 실행) ======

    @Nested
    @DisplayName("PDF 파일 기반 파싱 (로컬 전용)")
    class PdfFileParsingTest {

        private final PdfTextExtractor extractor = new PdfTextExtractor();

        @Test
        @DisplayName("test1.pdf 파싱 시 과목 목록이 추출된다")
        void parse_test1_pdf() throws IOException {
            ParsedTranscriptData result = parsePdfIfAvailable("pdf/test1.pdf");
            if (result == null) return;

            assertThat(result.courses()).isNotEmpty();
            assertThat(result.meta()).isNotEmpty();
            assertCreditsMatch(result, "test1.pdf");
        }

        @Test
        @DisplayName("test2.pdf 파싱 시 과목 목록이 추출된다")
        void parse_test2_pdf() throws IOException {
            ParsedTranscriptData result = parsePdfIfAvailable("pdf/test2.pdf");
            if (result == null) return;

            assertThat(result.courses()).isNotEmpty();
            assertCreditsMatch(result, "test2.pdf");
        }

        @Test
        @DisplayName("test3.pdf 파싱 시 과목 목록이 추출된다")
        void parse_test3_pdf() throws IOException {
            ParsedTranscriptData result = parsePdfIfAvailable("pdf/test3.pdf");
            if (result == null) return;

            assertThat(result.courses()).isNotEmpty();
            assertCreditsMatch(result, "test3.pdf");
        }

        @Test
        @DisplayName("test4.pdf 파싱 시 과목 목록이 추출된다")
        void parse_test4_pdf() throws IOException {
            ParsedTranscriptData result = parsePdfIfAvailable("pdf/test4.pdf");
            if (result == null) return;

            assertThat(result.courses()).isNotEmpty();
            assertCreditsMatch(result, "test4.pdf");
        }

        @Test
        @DisplayName("test-dup.pdf 파싱 시 복수전공 과목과 총학점이 일치한다")
        void parse_dual_major_pdf() throws IOException {
            ParsedTranscriptData result = parsePdfIfAvailable("pdf/test-dup.pdf");
            if (result == null) return;

            assertThat(result.meta().get("복수1")).isNotBlank();
            assertThat(result.meta().get("복수1기준학기")).matches("\\d{4}-(1|2|여름|겨울)");
            assertThat(result.courses())
                    .filteredOn(course -> "복수1".equals(course.category()))
                    .isNotEmpty();
            assertCreditsMatch(result, "test-dup.pdf");
        }

        private void assertCreditsMatch(ParsedTranscriptData result, String fileName) {
            String metaTotalStr = result.meta().get("총취득학점");
            if (metaTotalStr != null) {
                int metaTotal = Integer.parseInt(metaTotalStr);
                int parsedTotal = result.courses().stream()
                        .mapToInt(ParsedCourse::credits)
                        .sum();
                assertThat(parsedTotal)
                        .as("%s: 메타 총학점=%d, 파싱 합계=%d", fileName, metaTotal, parsedTotal)
                        .isEqualTo(metaTotal);
            }
        }

        /**
         * PDF 리소스가 존재하면 파싱 결과를 반환하고, 없으면 테스트를 스킵한다.
         */
        private ParsedTranscriptData parsePdfIfAvailable(String resourcePath) throws IOException {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            assumeTrue(is != null, resourcePath + " 파일이 없어 스킵합니다 (로컬에 PDF를 배치하면 실행됩니다)");

            byte[] pdfBytes = is.readAllBytes();
            is.close();
            String text = extractor.extract(pdfBytes);
            return parser.parse(text);
        }
    }
}
