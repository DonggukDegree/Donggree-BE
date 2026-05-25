package com.donggree.transcript.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TranscriptTest {

    // --- Transcript 생성 테스트 ---

    @Test
    void Transcript_생성_시_필수_필드가_올바르게_저장된다() {
        Transcript transcript = createTranscript();

        assertThat(transcript.getMemberId()).isEqualTo(1L);
        assertThat(transcript.getPdfUrl()).isEqualTo("https://storage.example.com/test.pdf");
        assertThat(transcript.getRawData()).isEqualTo("{\"pages\": []}");
        assertThat(transcript.getAdmissionYear()).isEqualTo(2023);
        assertThat(transcript.getAcademicStatus()).isEqualTo("재학");
        assertThat(transcript.getTotalCredits()).isZero();
        assertThat(transcript.getGpa()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(transcript.getCompletedSemesters()).isZero();
        assertThat(transcript.isEngineeringCertified()).isFalse();
        assertThat(transcript.isTransfer()).isFalse();
        assertThat(transcript.isSelectiveCompletion()).isFalse();
        assertThat(transcript.isGlobalTalentTrack()).isFalse();
        assertThat(transcript.isEnglishCourseTarget()).isFalse();
        assertThat(transcript.isThesisStatus()).isFalse();
        assertThat(transcript.getCourseRecords()).isEmpty();
        assertThat(transcript.isDeleted()).isFalse();
    }

    @Test
    void memberId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> Transcript.create(
                null, "https://storage.example.com/test.pdf",
                "{}", 2023, "재학"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberId");
    }

    // --- addCourseRecord 테스트 ---

    @Test
    void 수강_이력_추가_시_양방향_관계가_설정된다() {
        Transcript transcript = createTranscript();
        CourseRecord record = CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);

        transcript.addCourseRecord(record);

        assertThat(transcript.getCourseRecords()).hasSize(1);
        assertThat(transcript.getCourseRecords().get(0)).isSameAs(record);
        assertThat(record.getTranscript()).isSameAs(transcript);
    }

    @Test
    void 여러_수강_이력을_추가할_수_있다() {
        Transcript transcript = createTranscript();
        CourseRecord record1 = CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);
        CourseRecord record2 = CourseRecord.create(
                "2023-1", CourseType.COMMON_GENERAL, 2L, 20L, Grade.B_ZERO, false);
        CourseRecord record3 = CourseRecord.create(
                "2023-2", CourseType.LIBERAL_ARTS, null, 30L, Grade.P, false);

        transcript.addCourseRecord(record1);
        transcript.addCourseRecord(record2);
        transcript.addCourseRecord(record3);

        assertThat(transcript.getCourseRecords()).hasSize(3);
    }

    @Test
    void null_수강_이력을_추가하면_예외가_발생한다() {
        Transcript transcript = createTranscript();

        assertThatThrownBy(() -> transcript.addCourseRecord(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCourseRecords는_불변_리스트를_반환한다() {
        Transcript transcript = createTranscript();
        CourseRecord record = CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);
        transcript.addCourseRecord(record);

        assertThatThrownBy(() -> transcript.getCourseRecords().add(
                CourseRecord.create("2023-2", CourseType.COMMON_GENERAL,
                        null, 20L, Grade.B_PLUS, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- 소프트 삭제 테스트 ---

    @Test
    void markAsDeleted_호출_시_deletedAt이_기록된다() {
        Transcript transcript = createTranscript();

        transcript.markAsDeleted();

        assertThat(transcript.isDeleted()).isTrue();
        assertThat(transcript.getDeletedAt()).isNotNull();
    }

    @Test
    void 이미_삭제된_Transcript에_markAsDeleted_호출_시_예외가_발생한다() {
        Transcript transcript = createTranscript();
        transcript.markAsDeleted();

        assertThatThrownBy(transcript::markAsDeleted)
                .isInstanceOf(IllegalStateException.class);
    }

    // --- CourseRecord 생성 테스트 ---

    @Test
    void CourseRecord_생성_시_필드가_올바르게_저장된다() {
        CourseRecord record = CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, true);

        assertThat(record.getSemester()).isEqualTo("2023-1");
        assertThat(record.getCourseType()).isEqualTo(CourseType.FIRST_MAJOR);
        assertThat(record.getAreaTypeId()).isEqualTo(1L);
        assertThat(record.getCourseId()).isEqualTo(10L);
        assertThat(record.getGrade()).isEqualTo(Grade.A_PLUS);
        assertThat(record.isRetake()).isTrue();
    }

    @Test
    void CourseRecord_생성_시_grade가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, 10L, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grade");
    }

    // --- Grade enum 테스트 ---

    @Test
    void Grade_enum의_value가_올바르게_매핑된다() {
        assertThat(Grade.A_PLUS.getValue()).isEqualTo("A+");
        assertThat(Grade.A_ZERO.getValue()).isEqualTo("A0");
        assertThat(Grade.B_PLUS.getValue()).isEqualTo("B+");
        assertThat(Grade.B_ZERO.getValue()).isEqualTo("B0");
        assertThat(Grade.C_PLUS.getValue()).isEqualTo("C+");
        assertThat(Grade.C_ZERO.getValue()).isEqualTo("C0");
        assertThat(Grade.D_PLUS.getValue()).isEqualTo("D+");
        assertThat(Grade.D_ZERO.getValue()).isEqualTo("D0");
        assertThat(Grade.F.getValue()).isEqualTo("F");
        assertThat(Grade.P.getValue()).isEqualTo("P");
    }

    @Test
    void Grade_fromValue로_문자열에서_enum을_복원할_수_있다() {
        assertThat(Grade.fromValue("A+")).isEqualTo(Grade.A_PLUS);
        assertThat(Grade.fromValue("B0")).isEqualTo(Grade.B_ZERO);
        assertThat(Grade.fromValue("F")).isEqualTo(Grade.F);
        assertThat(Grade.fromValue("P")).isEqualTo(Grade.P);
    }

    @Test
    void Grade_fromValue에_존재하지_않는_값을_넘기면_예외가_발생한다() {
        assertThatThrownBy(() -> Grade.fromValue("X"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- 헬퍼 메서드 ---

    private Transcript createTranscript() {
        return Transcript.create(1L, "https://storage.example.com/test.pdf",
                "{\"pages\": []}", 2023, "재학");
    }
}
