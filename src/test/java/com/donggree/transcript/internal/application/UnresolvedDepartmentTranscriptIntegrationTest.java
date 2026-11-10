package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.transcript.internal.application.command.CourseRecordCreateCommand;
import com.donggree.transcript.internal.application.command.ParsedCourse;
import com.donggree.transcript.internal.application.command.ParsedTranscriptData;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.application.pdf.TranscriptPdfReader;
import com.donggree.transcript.internal.application.query.TranscriptQueryService;
import com.donggree.transcript.internal.application.query.TranscriptViewMapper;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.domain.transcript.TranscriptRepository;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.donggree.transcript.internal.infrastructure.PdfTranscriptTextParser;
import com.donggree.user.UserIdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 실제 운영 DB와 분리된 H2에서 미식별 학과의 저장·조회·수정 및 회원별 격리 검증. */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:unresolved-transcript;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaAuditingConfig.class,
    TranscriptCommandService.class,
    TranscriptQueryService.class,
    TranscriptViewMapper.class,
    TranscriptPdfReader.class,
    PdfTextExtractor.class,
    PdfTranscriptTextParser.class,
    ObjectMapper.class
})
class UnresolvedDepartmentTranscriptIntegrationTest {
    @Autowired
    private TranscriptCommandService commandService;

    @Autowired
    private TranscriptQueryService queryService;

    @Autowired
    private TranscriptRepository repository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UserIdentityService memberIdentityService;

    @Test
    void 추론한_NP는_저장조회수정_전체에서_평점만_제외하고_이수로_인정하지_않는다() {
        var parsedSource = new ParsedTranscriptData(
                Map.of("교육과정 적용년도", "2023", "총취득학점", "1", "평점평균", "4.00"),
                List.of(
                        new ParsedCourse("2025-1", 1, "전선", "777700", "통과과목", 1, "A0", "전문", false),
                        new ParsedCourse("2025-2", 1, "전선", "777700", "실패과목", 1, "F", "전문", false)));
        var reader = new TranscriptPdfReader(bytes -> "원문", new ObjectMapper(), text -> parsedSource);
        var parsed = reader.parseTranscript(new byte[0]);
        var createData =
                reader.buildCreateData(8L, parsed.rawDataJson(), parsed.parsedData(), null, null, null, null, null);
        var courses = parsed.parsedData().courses().stream()
                .map(c -> new CourseRecordCreateCommand(
                        c.semester(),
                        c.category(),
                        c.area(),
                        c.courseCode(),
                        c.courseName(),
                        c.credits(),
                        Grade.fromValue(c.grade()),
                        c.retake()))
                .toList();
        assertThat(commandService
                        .createTranscript(createData, courses, "2023123456", "홍길동")
                        .creditGap())
                .isZero();
        repository.flush();
        entityManager.clear();
        var stored = repository.findByMemberId(8L).orElseThrow();
        assertThat(stored.getCourseRecords()).extracting(c -> c.getGrade()).containsExactly(Grade.A_ZERO, Grade.NP);
        var view = new TranscriptViewMapper(new ObjectMapper()).toView(stored);
        assertThat(view.courseRecords()).extracting(c -> c.passed()).containsExactly(true, false);
        assertThat(queryService.getTranscriptRawReport(8L).meta().majorGpa()).isEqualByComparingTo("4.00");
        assertThat(queryService
                        .getTranscriptRawReport(8L)
                        .semesterGroups()
                        .get(1)
                        .records()
                        .getFirst()
                        .grade())
                .isEqualTo("NP");

        // 사용자가 F(등급)으로 바꾼 경우에도 원문은 보존하고, 취득학점은 그대로·평점만 변경한다.
        var failed = courses.get(1);
        var changed = new CourseRecordCreateCommand(
                failed.semester(),
                failed.courseTypeName(),
                failed.areaName(),
                failed.courseCode(),
                failed.courseName(),
                failed.credits(),
                Grade.F,
                failed.retake());
        var updated = commandService.replaceCourseRecords(8L, List.of(courses.getFirst(), changed));
        assertThat(updated.totalCredits()).isEqualTo(1);
        assertThat(updated.gpa()).isEqualByComparingTo("2.00");
        entityManager.clear();
        assertThat(repository.findByMemberId(8L).orElseThrow().getRawData()).isEqualTo(parsed.rawDataJson());
    }

    @Test
    void 학과_ID_없이_저장한_성적표를_본인만_조회하고_수정한다() {
        String rawData = "{\"meta\":{\"학과\":\"미등록학과\",\"대학\":\"미등록대학\"}}";
        var parsed = new ParsedTranscriptData(
                Map.of(
                        "학과",
                        "미등록학과",
                        "대학",
                        "미등록대학",
                        "학적상태",
                        "재학",
                        "교육과정 적용년도",
                        "2023",
                        "총취득학점",
                        "3",
                        "평점평균",
                        "4.50",
                        "이수학기",
                        "1"),
                List.of());
        var data = commandService.buildCreateData(7L, rawData, parsed, null, null, null, null, null);

        var saved = commandService.createTranscript(
                data,
                List.of(new CourseRecordCreateCommand("2023-1", "전공", "기초", "CSE1", "과목", 3, Grade.A_PLUS, false)),
                "2023123456",
                "홍길동");
        repository.flush();
        entityManager.clear();

        assertThat(saved.totalCredits()).isEqualTo(3);
        assertThat(saved.creditGap()).isZero();
        var loaded = queryService.getTranscriptRawReport(7L);
        assertThat(loaded.meta().departmentId()).isNull();
        assertThat(loaded.meta().pdfDepartment()).isEqualTo("미등록학과");
        assertThat(loaded.meta().pdfCollegeName()).isEqualTo("미등록대학");
        assertThat(loaded.meta().majorGpa()).isEqualByComparingTo("4.50");
        assertThat(loaded.semesterGroups().getFirst().records())
                .singleElement()
                .satisfies(course -> assertThat(course.courseCode()).isEqualTo("CSE1"));
        assertThatThrownBy(() -> queryService.getTranscriptRawReport(8L))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(TranscriptErrorCode.TRANSCRIPT_NOT_FOUND));
        then(memberIdentityService).should().verifyIdentityIfMatch(7L, "2023123456", "홍길동");

        commandService.replaceCourseRecords(
                7L, List.of(new CourseRecordCreateCommand("2023-1", "전공", "기초", "CSE1", "과목", 3, Grade.B_ZERO, false)));
        entityManager.clear();

        assertThat(queryService.getTranscriptRawReport(7L).meta().majorGpa()).isEqualByComparingTo("3.00");
        assertThat(repository.findByMemberId(7L).orElseThrow().getRawData()).isEqualTo(rawData);
    }
}
