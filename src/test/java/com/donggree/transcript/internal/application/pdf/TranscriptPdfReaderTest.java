package com.donggree.transcript.internal.application.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.transcript.internal.application.command.ParsedCourse;
import com.donggree.transcript.internal.application.command.ParsedTranscriptData;
import com.donggree.transcript.internal.application.query.TranscriptViewMapper;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.domain.transcript.Transcript;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TranscriptPdfReaderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ParsedCourse course(String semester, String grade) {
        return new ParsedCourse(semester, 1, "전선", "777700", "교류과목", 1, grade, "전문", false);
    }

    private TranscriptPdfReader reader(List<ParsedCourse> courses, String gpa, String credits) {
        return new TranscriptPdfReader(
                bytes -> "원문 보존",
                mapper,
                text -> new ParsedTranscriptData(Map.of("총취득학점", credits, "평점평균", gpa, "교육과정 적용년도", "2023"), courses));
    }

    @Test
    void 추론된_NP는_저장과_미리보기에_적용하되_원문_JSON은_F로_보존한다() throws Exception {
        var courses = List.of(course("2025-1", "A0"), course("2025-2", "F"));
        var reader = reader(courses, "4.00", "1");
        var parsed = reader.parseTranscript(new byte[0]);
        assertThat(parsed.parsedData().courses())
                .extracting(ParsedCourse::grade)
                .containsExactly("A0", "NP");
        assertThat(parsed.gradeReviewCourses()).isEmpty();
        assertThat(mapper.readTree(parsed.rawDataJson())
                        .path("courses")
                        .get(1)
                        .path("grade")
                        .asText())
                .isEqualTo("F");
        assertThat(courses.get(1).grade()).isEqualTo("F");

        var data = reader.buildCreateData(1L, parsed.rawDataJson(), parsed.parsedData(), null, null, null, null, null);
        var preview = new TranscriptViewMapper(mapper)
                .toPreview(data, parsed.parsedData().courses());
        assertThat(preview.gpa()).isEqualByComparingTo("4.00");
        assertThat(preview.majorGpa()).isEqualByComparingTo("4.00");
        assertThat(preview.courseRecords()).extracting(c -> c.passed()).containsExactly(true, false);
        var transcript = Transcript.create(data);
        parsed.parsedData()
                .courses()
                .forEach(c -> transcript.addCourseRecord(
                        c.semester(),
                        c.category(),
                        c.area(),
                        c.courseCode(),
                        c.courseName(),
                        c.credits(),
                        Grade.fromValue(c.grade()),
                        c.retake()));
        var savedView = new TranscriptViewMapper(mapper).toView(transcript);
        assertThat(savedView.majorGpa()).isEqualByComparingTo(preview.majorGpa());
        assertThat(transcript.recordedCredits()).isEqualTo(1);
    }

    @Test
    void 모호한_동일학수번호의_행들을_학기와_함께_각각_안내한다() {
        var parsed = reader(List.of(course("2024-1", "A0"), course("2025-1", "F"), course("2025-2", "F")), "2.00", "1")
                .parseTranscript(new byte[0]);
        assertThat(parsed.parsedData().courses())
                .extracting(ParsedCourse::grade)
                .containsExactly("A0", "F", "F");
        assertThat(parsed.gradeReviewCourses()).extracting(c -> c.semester()).containsExactly("2025-1", "2025-2");
    }

    @Test
    void 파싱누락으로_취득학점이_다르면_평점에_억지로_맞추지_않는다() {
        var parsed = reader(List.of(course("2024-1", "A0"), course("2025-1", "F")), "4.00", "3")
                .parseTranscript(new byte[0]);
        assertThat(parsed.parsedData().courses())
                .extracting(ParsedCourse::grade)
                .containsExactly("A0", "F");
        assertThat(parsed.gradeReviewCourses()).hasSize(1);
    }
}
