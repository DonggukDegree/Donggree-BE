package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.transcript.internal.application.command.CourseRecordCreateCommand;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.donggree.user.MemberIdentityService;
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
    private MemberIdentityService memberIdentityService;

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
