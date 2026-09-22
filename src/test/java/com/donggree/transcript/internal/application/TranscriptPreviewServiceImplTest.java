package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.donggree.user.MemberIdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TranscriptPreviewServiceImplTest {
    private final byte[] pdf = {1, 2, 3};
    private final PdfTextExtractor extractor = mock(PdfTextExtractor.class);
    private final CurriculumLookupService curriculum = mock(CurriculumLookupService.class);
    private final TranscriptRepository repository = mock(TranscriptRepository.class);
    private final MemberIdentityService identity = mock(MemberIdentityService.class);
    private final TranscriptPdfReader reader = new TranscriptPdfReader(extractor, new ObjectMapper());
    private final TranscriptViewMapper mapper = new TranscriptViewMapper(new ObjectMapper());
    private final TranscriptPreviewServiceImpl service = new TranscriptPreviewServiceImpl(reader, mapper, curriculum);

    @BeforeEach
    void setUp() {
        given(curriculum.findDepartmentIdByName("테스트학부")).willReturn(Optional.of(10L));
        given(curriculum.findDepartmentIdByName("테스트주전공")).willReturn(Optional.of(10L));
        given(curriculum.findDepartmentIdByName("테스트복수전공")).willReturn(Optional.of(20L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"transcript-fixture.txt", "transcript-dual-major-fixture.txt"})
    void 저장_경로와_같은_파서와_판정_입력_사용(String fixture) throws Exception {
        given(extractor.extract(pdf)).willReturn(fixture(fixture));
        var preview = service.preview(pdf);
        var command = new TranscriptCommandService(repository, reader, identity);
        var parsed = command.parseTranscript(pdf);
        var data = command.buildCreateData(
                1L,
                parsed.rawDataJson(),
                parsed.parsedData(),
                preview.departmentId(),
                preview.subMajor1Id(),
                preview.subMajor2Id(),
                preview.dualMajor1Id(),
                preview.dualMajor2Id());
        // 저장 경로에서 만드는 엔티티와 비교하되, DB 저장 자체는 하지 않는다.
        var transcript = Transcript.create(data);
        for (var course : parsed.parsedData().courses()) {
            transcript.addCourseRecord(
                    course.semester(),
                    course.category(),
                    course.area() == null || course.area().isBlank() ? null : course.area(),
                    course.courseCode(),
                    course.courseName(),
                    course.credits(),
                    Grade.fromValue(course.grade()),
                    course.retake());
        }
        assertThat(preview)
                .usingRecursiveComparison()
                .ignoringFields("id", "memberId")
                .isEqualTo(mapper.toView(transcript));
        assertThat(preview.id()).isNull();
        assertThat(preview.memberId()).isNull();
        verifyNoInteractions(repository, identity);
    }

    @Test
    void 이름_학번_학적상태_등록학기가_없어도_미리보기_허용() throws Exception {
        String text = fixture("transcript-fixture.txt")
                .replace("2023000001", "")
                .replace("홍길동", "")
                .replace("학적상태: 재학", "학적상태:")
                .replaceAll("(?m)^202[34] - [12] [12]학년 등필$", "");
        given(extractor.extract(pdf)).willReturn(text);
        assertThat(service.preview(pdf).courseRecords()).hasSize(12);
        verifyNoInteractions(repository, identity);
    }

    @Test
    void 다른_PDF_교체_시_직전_결과_미사용() throws Exception {
        given(extractor.extract(pdf))
                .willReturn(fixture("transcript-fixture.txt"), fixture("transcript-dual-major-fixture.txt"));
        var first = service.preview(pdf);
        var next = service.preview(pdf);
        assertThat(first.dualMajor1Id()).isNull();
        assertThat(next.dualMajor1Id()).isEqualTo(20L);
        assertThat(next.courseRecords()).hasSize(5);
        verifyNoInteractions(repository, identity);
    }

    @Test
    void 미이수_성적과_복수전공_시험_결과_보존() throws Exception {
        given(extractor.extract(pdf))
                .willReturn(fixture("transcript-dual-major-fixture.txt")
                        .replace("3 A+ 전문", "3 F 전문")
                        .replace("3 B+ 기초", "3 NP 기초")
                        .replace("심사(복수1): 미판정", "심사(복수1): 합격"));
        var view = service.preview(pdf);
        assertThat(view.thesisStatus()).isFalse();
        assertThat(view.dualMajor1ThesisStatus()).isTrue();
        assertThat(view.courseRecords()).filteredOn(record -> !record.passed()).hasSize(3);
    }

    @Test
    void 파일_없음_거부() {
        assertError(() -> service.preview(null), TranscriptErrorCode.PDF_FILE_REQUIRED);
        assertError(() -> service.preview(new byte[0]), TranscriptErrorCode.PDF_FILE_REQUIRED);
        verifyNoInteractions(extractor);
    }

    @Test
    void 잘못된_PDF_거부() throws Exception {
        given(extractor.extract(pdf)).willThrow(new IOException("invalid PDF"));
        assertError(() -> service.preview(pdf), TranscriptErrorCode.INVALID_PDF_FILE);
    }

    @Test
    void 판정용_학사정보가_없는_문서_거부() throws Exception {
        given(extractor.extract(pdf)).willReturn("취득교과목 영역별 분류표");
        assertError(() -> service.preview(pdf), TranscriptErrorCode.PDF_PARSING_FAILED);
    }

    @Test
    void 등록되지_않은_주전공_거부() throws Exception {
        given(extractor.extract(pdf))
                .willReturn(fixture("transcript-fixture.txt").replace("테스트학부", "미등록학과"));
        assertError(() -> service.preview(pdf), TranscriptErrorCode.DEPARTMENT_NOT_FOUND);
    }

    private void assertError(Runnable action, TranscriptErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(code));
    }

    private String fixture(String name) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
