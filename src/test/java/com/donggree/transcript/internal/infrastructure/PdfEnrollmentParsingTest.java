package com.donggree.transcript.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.transcript.internal.application.command.ParsedCourse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfEnrollmentParsingTest {
    private final PdfTranscriptTextParser parser = new PdfTranscriptTextParser();

    @Test
    void 수강신청의_이전성적을_제외하고_옆열의_취득과목은_보존한다() throws Exception {
        // Helvetica의 테스트 전용 Unicode 매핑으로 개인정보·외부 글꼴 없는 3열 PDF를 만든다.
        try (var doc = new PDDocument();
                var out = new ByteArrayOutputStream()) {
            var page = new PDPage(new PDRectangle(842, 595));
            doc.addPage(page);
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var labels = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String cmap =
                    """
                    /CIDInit /ProcSet findresource begin 12 dict begin begincmap
                    /CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def
                    /CMapName /Test def /CMapType 2 def
                    1 begincodespacerange <00> <FF> endcodespacerange
                    6 beginbfchar
                    <59> <B144> <48> <D559> <6D> <C218> <6B> <AC15> <73> <C2E0> <63> <CCAD>
                    endbfchar endcmap CMapName currentdict /CMap defineresource pop end end
                    """;
            var stream = new PDStream(doc);
            try (var output = stream.createOutputStream()) {
                output.write(cmap.getBytes(StandardCharsets.US_ASCII));
            }
            labels.getCOSObject().setItem(COSName.TO_UNICODE, stream);
            try (var content = new PDPageContentStream(doc, page)) {
                for (float x : new float[] {25, 292, 559}) write(content, labels, x, 460, "YH");
                write(
                        content,
                        font,
                        22,
                        430,
                        "2024-1 1 전공 CSE1001 Completed 3 A+ 기초"
                                .replace("전공", "X")
                                .replace("기초", ""));
                write(content, labels, 555, 410, "<<mksc>>");
                write(content, font, 288, 390, "2025-1 2 X CSE2001 Completed 3 A+ ");
                write(content, font, 555, 390, "2026-2 3 X CSE1001 Pending 3 A+ ");
            }
            doc.save(out);
            String text = new PdfTextExtractor().extract(out.toByteArray()).replace(" X ", " 전공 ");
            assertThat(text).contains("<<수강신청>>", "Pending");
            var courses = parser.parse(text).courses();
            assertThat(courses).extracting(ParsedCourse::courseCode).containsExactly("CSE1001", "CSE2001");
            assertThat(courses).extracting(ParsedCourse::semester).containsExactly("2024-1", "2025-1");
        }
    }

    @Test
    void 영역과_신규이수구분_영학점_숫자코드를_보존한다() {
        var data = parser.parse(
                """
                2025-1 1 공교 RGC1090 디지털과목 3 P 디지
                2025-1 1 공교 RGC2105 인공지능과목 3 A+ AI
                2025-1 1 공교 RGC2301 지구과목 2 A0 지구
                2025-1 1 공교 RGC1079 진로과목 1 P 진로
                2025-1 1 공교 RGC2201 문화과목 2 B+ 문화
                2025-1 1 공교 RGC1075 지역과목 2 A+ 지역
                2025-1 1 일교 DEV11079 창업과목 2 P 자기
                2025-1 1 전선 CIV4001 전공선택 3 A+ 전문
                2025-1 1 대기 SSC2008 국제통상의이해 3 B+
                2025-1 1 나노 NAD1001 나노과목 1 A+ 이론
                2025-공통 1 자선 7777700 영학점과목 0 P
                2025-공통 1 자선 7777700 다른교류과목 1 P
                """);
        assertThat(data.courses()).hasSize(12);
        assertThat(data.courses())
                .extracting(ParsedCourse::area)
                .contains("디지", "AI", "지구", "진로", "문화", "지역", "자기", "전문", "이론");
        assertThat(data.courses())
                .filteredOn(c -> c.courseCode().equals("7777700"))
                .hasSize(2);
        assertThat(data.courses())
                .filteredOn(c -> c.courseName().equals("영학점과목"))
                .singleElement()
                .satisfies(c -> assertThat(c.credits()).isZero());
    }

    private static void write(PDPageContentStream content, PDType1Font font, float x, float y, String text)
            throws Exception {
        content.beginText();
        content.setFont(font, 8);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}
