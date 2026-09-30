package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.donggree.transcript.internal.domain.CourseRecordData;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TranscriptQueryServiceTest {

    private final TranscriptRepository repository = mock(TranscriptRepository.class);
    private final TranscriptQueryService service =
            new TranscriptQueryService(repository, new TranscriptViewMapper(new ObjectMapper()));

    @Test
    void PDF_요약값이_아닌_현재_과목의_전공별_평점을_조회하고_저장하지_않는다() {
        Transcript transcript = givenTranscript(20L);
        transcript.addCourseRecord("2023-1", "전필", null, "CSE1", "주전공", 3, Grade.A_PLUS, false);
        transcript.addCourseRecord("2023-1", "복수1", null, "DUAL1", "복수전공", 3, Grade.A_ZERO, false);

        var result = service.getTranscriptRawReport(1L);

        assertThat(result.meta().majorGpa()).isEqualByComparingTo("4.50");
        assertThat(result.meta().dualMajor1Gpa()).isEqualByComparingTo("4.00");
        assertThat(result.meta().gpa()).isEqualByComparingTo("3.50");
        assertThat(result.meta().totalCredits()).isEqualTo(60);
        assertThat(result.semesterGroups().getFirst().records()).hasSize(2);
        then(repository).should().findByMemberId(1L);
        then(repository).shouldHaveNoMoreInteractions();
    }

    @Test
    void 수강내역_수정과_삭제_후_재조회에_전공별_평점이_반영된다() {
        Transcript transcript = givenTranscript(20L);
        String originalRawData = transcript.getRawData();
        transcript.addCourseRecord("2023-1", "전공", null, "CSE1", "주전공", 3, Grade.A_PLUS, false);
        transcript.addCourseRecord("2023-1", "복수1", null, "DUAL1", "복수전공", 3, Grade.A_ZERO, false);
        assertThat(service.getTranscriptRawReport(1L).meta().majorGpa()).isEqualByComparingTo("4.50");

        transcript.replaceCourseRecords(List.of(
                new CourseRecordData("2023-1", "전필", null, "CSE1", "주전공", 3, Grade.B_ZERO, false),
                new CourseRecordData("2023-1", "복수1", null, "DUAL1", "복수전공", 3, Grade.C_ZERO, false)));

        var updated = service.getTranscriptRawReport(1L).meta();
        assertThat(updated.majorGpa()).isEqualByComparingTo("3.00");
        assertThat(updated.dualMajor1Gpa()).isEqualByComparingTo("2.00");
        assertThat(updated.gpa()).isEqualByComparingTo("2.50");

        transcript.replaceCourseRecords(List.of());
        var empty = service.getTranscriptRawReport(1L).meta();
        assertThat(empty.majorGpa()).isNull();
        assertThat(empty.dualMajor1Gpa()).isNull();
        assertThat(empty.gpa()).isEqualByComparingTo("0.00");
        assertThat(transcript.getRawData()).isEqualTo(originalRawData);
    }

    @Test
    void 단일전공자와_성적_없는_사용자는_복수전공평점이_null이다() {
        givenTranscript(null);

        var meta = service.getTranscriptRawReport(1L).meta();

        assertThat(meta.dualMajor1Id()).isNull();
        assertThat(meta.majorGpa()).isNull();
        assertThat(meta.dualMajor1Gpa()).isNull();
    }

    @Test
    void 미식별_학과도_원문_이름과_본인_수강내역을_조회한다() {
        String rawData = "{\"meta\":{\"학과\":\" 미등록학과 \",\"대학\":\" 미등록대학 \"}}";
        Transcript transcript = givenTranscript(null, null, rawData);
        transcript.addCourseRecord("2023-1", "전공", "기초", "CSE1", "과목", 3, Grade.A_PLUS, false);

        var result = service.getTranscriptRawReport(1L);

        assertThat(result.meta().departmentId()).isNull();
        assertThat(result.meta().pdfDepartment()).isEqualTo("미등록학과");
        assertThat(result.meta().pdfCollegeName()).isEqualTo("미등록대학");
        assertThat(result.meta().majorGpa()).isEqualByComparingTo("4.50");
        assertThat(result.semesterGroups()).hasSize(1);
        assertThat(transcript.getRawData()).isEqualTo(rawData);
        then(repository).should().findByMemberId(1L);
        then(repository).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"{}", "{\"meta\":{\"학과\":\"  \",\"대학\":null}}", "{\"meta\":{\"학과\":123}}", "null", "broken-json"
            })
    void 원문_이름이_없거나_읽을_수_없어도_학업정보_조회는_유지한다(String rawData) {
        givenTranscript(null, null, rawData);

        var meta = service.getTranscriptRawReport(1L).meta();

        assertThat(meta.pdfDepartment()).isNull();
        assertThat(meta.pdfCollegeName()).isNull();
        assertThat(meta.totalCredits()).isEqualTo(60);
    }

    private Transcript givenTranscript(Long dualMajor1Id) {
        return givenTranscript(10L, dualMajor1Id, "{\"meta\":{\"제1전공평점\":\"1.00\",\"복수1평점\":\"2.00\"}}");
    }

    private Transcript givenTranscript(Long departmentId, Long dualMajor1Id, String rawData) {
        Transcript transcript = Transcript.create(new TranscriptCreateData(
                1L,
                rawData,
                2023,
                "재학",
                "학사과정",
                departmentId,
                null,
                null,
                dualMajor1Id,
                null,
                60,
                new BigDecimal("3.50"),
                4,
                null,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                false));
        given(repository.findByMemberId(1L)).willReturn(Optional.of(transcript));
        return transcript;
    }
}
