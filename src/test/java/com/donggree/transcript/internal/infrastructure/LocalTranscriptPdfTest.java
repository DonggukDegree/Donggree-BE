package com.donggree.transcript.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.donggree.transcript.internal.application.command.ParsedCourse;
import com.donggree.transcript.internal.application.command.ParsedTranscriptData;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Git 제외된 실제 PDF가 있는 환경에서 추출기와 파서를 함께 확인한다. */
@Tag("local-pdf")
class LocalTranscriptPdfTest {
    private final PdfTranscriptTextParser parser = new PdfTranscriptTextParser();
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    @DisplayName("test1.pdf 파싱 시 과목 목록이 추출된다")
    void parse_test1_pdf() throws IOException {
        ParsedTranscriptData result = parsePdfIfAvailable("pdf/test1.pdf");

        assertThat(result.courses()).isNotEmpty();
        assertThat(result.meta()).isNotEmpty();
        assertCreditsMatch(result, "test1.pdf");
    }

    @Test
    @DisplayName("test2.pdf 파싱 시 과목 목록이 추출된다")
    void parse_test2_pdf() throws IOException {
        ParsedTranscriptData result = parsePdfIfAvailable("pdf/test2.pdf");

        assertThat(result.courses()).isNotEmpty();
        assertCreditsMatch(result, "test2.pdf");
    }

    @Test
    @DisplayName("test3.pdf 파싱 시 과목 목록이 추출된다")
    void parse_test3_pdf() throws IOException {
        ParsedTranscriptData result = parsePdfIfAvailable("pdf/test3.pdf");

        assertThat(result.courses()).isNotEmpty();
        assertCreditsMatch(result, "test3.pdf");
    }

    @Test
    @DisplayName("test4.pdf 파싱 시 과목 목록이 추출된다")
    void parse_test4_pdf() throws IOException {
        ParsedTranscriptData result = parsePdfIfAvailable("pdf/test4.pdf");

        assertThat(result.courses()).isNotEmpty();
        assertCreditsMatch(result, "test4.pdf");
    }

    @Test
    @DisplayName("test-dup.pdf 파싱 시 복수전공 과목과 총학점이 일치한다")
    void parse_dual_major_pdf() throws IOException {
        ParsedTranscriptData result = parsePdfIfAvailable("pdf/test-dup.pdf");

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
            int parsedTotal =
                    result.courses().stream().mapToInt(ParsedCourse::credits).sum();
            assertThat(parsedTotal)
                    .as("%s: 메타 총학점=%d, 파싱 합계=%d", fileName, metaTotal, parsedTotal)
                    .isEqualTo(metaTotal);
        }
    }

    /**
     * PDF 리소스가 존재하면 파싱 결과를 반환하고, 없으면 테스트를 스킵한다.
     */
    private ParsedTranscriptData parsePdfIfAvailable(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assumeTrue(is != null, resourcePath + " 파일이 없어 스킵합니다 (로컬에 PDF를 배치하면 실행됩니다)");
            return parser.parse(extractor.extract(is.readAllBytes()));
        }
    }
}
