package com.donggree.transcript.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TranscriptTest {

    // --- Transcript 생성 테스트 ---

    @Test
    void Transcript_생성_시_모든_필드가_올바르게_저장된다() {
        Transcript transcript = createFullTranscript();

        assertThat(transcript.getMemberId()).isEqualTo(1L);
        assertThat(transcript.getRawData()).isEqualTo("{\"pages\": []}");
        assertThat(transcript.getAdmissionYear()).isEqualTo(2023);
        assertThat(transcript.getAcademicStatus()).isEqualTo("재학");
        assertThat(transcript.getStudentType()).isEqualTo("단일");
        assertThat(transcript.getDepartmentId()).isEqualTo(100L);
        assertThat(transcript.getSubMajor1Id()).isNull();
        assertThat(transcript.getSubMajor2Id()).isNull();
        assertThat(transcript.getDualMajor1Id()).isNull();
        assertThat(transcript.getDualMajor2Id()).isNull();
        assertThat(transcript.getTotalCredits()).isEqualTo(80);
        assertThat(transcript.getGpa()).isEqualByComparingTo(new BigDecimal("3.95"));
        assertThat(transcript.getCompletedSemesters()).isEqualTo(4);
        assertThat(transcript.getEnglishLevel()).isEqualTo("S1");
        assertThat(transcript.isEngineeringCertified()).isTrue();
        assertThat(transcript.isTransfer()).isFalse();
        assertThat(transcript.isSelectiveCompletion()).isFalse();
        assertThat(transcript.isGlobalTalentTrack()).isFalse();
        assertThat(transcript.isEnglishCourseTarget()).isTrue();
        assertThat(transcript.getCompletedEnglishResult()).isTrue();
        assertThat(transcript.getCompletedEnglishMajor()).isEqualTo(2);
        assertThat(transcript.getCompletedEnglishNonMajor()).isEqualTo(1);
        assertThat(transcript.getTeachingAptitudeCount()).isNull();
        assertThat(transcript.isThesisStatus()).isFalse();
        assertThat(transcript.getCourseRecords()).isEmpty();
        assertThat(transcript.isDeleted()).isFalse();
    }

    @Test
    void memberId가_null이면_예외가_발생한다() {
        TranscriptCreateData data = new TranscriptCreateData(
                null, "{}",
                2023, "재학", null, null, null, null, null, null,
                0, BigDecimal.ZERO, 0, null,
                false, false, false, false, false,
                null, null, null, null, false);

        assertThatThrownBy(() -> Transcript.create(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberId");
    }

    @Test
    void rawData가_null이면_예외가_발생한다() {
        TranscriptCreateData data = new TranscriptCreateData(
                1L, null,
                2023, "재학", null, null, null, null, null, null,
                0, BigDecimal.ZERO, 0, null,
                false, false, false, false, false,
                null, null, null, null, false);

        assertThatThrownBy(() -> Transcript.create(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawData");
    }

    @Test
    void academicStatus가_null이면_예외가_발생한다() {
        TranscriptCreateData data = new TranscriptCreateData(
                1L, "{}",
                2023, null, null, null, null, null, null, null,
                0, BigDecimal.ZERO, 0, null,
                false, false, false, false, false,
                null, null, null, null, false);

        assertThatThrownBy(() -> Transcript.create(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("academicStatus");
    }

    // --- addCourseRecord 테스트 ---

    @Test
    void 수강_이력_추가_시_양방향_관계가_설정된다() {
        Transcript transcript = createTranscript();

        transcript.addCourseRecord("2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);

        assertThat(transcript.getCourseRecords()).hasSize(1);
        CourseRecord record = transcript.getCourseRecords().get(0);
        assertThat(record.getSemester()).isEqualTo("2023-1");
        assertThat(record.getCourseType()).isEqualTo(CourseType.FIRST_MAJOR);
        assertThat(record.getTranscript()).isSameAs(transcript);
    }

    @Test
    void 여러_수강_이력을_추가할_수_있다() {
        Transcript transcript = createTranscript();

        transcript.addCourseRecord("2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);
        transcript.addCourseRecord("2023-1", CourseType.COMMON_GENERAL, 2L, 20L, Grade.B_ZERO, false);
        transcript.addCourseRecord("2023-2", CourseType.LIBERAL_ARTS, null, 30L, Grade.P, false);

        assertThat(transcript.getCourseRecords()).hasSize(3);
    }

    @Test
    void getCourseRecords는_불변_리스트를_반환한다() {
        Transcript transcript = createTranscript();
        transcript.addCourseRecord("2023-1", CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false);

        assertThatThrownBy(() -> transcript.getCourseRecords().add(
                CourseRecord.create("2023-2", CourseType.COMMON_GENERAL,
                        null, 20L, Grade.B_PLUS, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- 소프트 삭제 테스트 ---

    @Test
    void markAsDeleted_호출_시_deletedAt이_기록된다() {
        Transcript transcript = createTranscript();
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        transcript.markAsDeleted(now);

        assertThat(transcript.isDeleted()).isTrue();
        assertThat(transcript.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void 이미_삭제된_Transcript에_markAsDeleted_호출_시_예외가_발생한다() {
        Transcript transcript = createTranscript();
        transcript.markAsDeleted(LocalDateTime.of(2025, 1, 1, 12, 0));

        assertThatThrownBy(() -> transcript.markAsDeleted(LocalDateTime.now()))
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

    @Test
    void CourseRecord_생성_시_semester가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> CourseRecord.create(
                null, CourseType.FIRST_MAJOR, 1L, 10L, Grade.A_PLUS, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semester");
    }

    @Test
    void CourseRecord_생성_시_courseType이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> CourseRecord.create(
                "2023-1", null, 1L, 10L, Grade.A_PLUS, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("courseType");
    }

    @Test
    void CourseRecord_생성_시_courseId가_null이어도_정상_생성된다() {
        CourseRecord record = CourseRecord.create(
                "2023-1", CourseType.FIRST_MAJOR, 1L, null, Grade.A_PLUS, false);

        assertThat(record.getCourseId()).isNull();
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
        return Transcript.create(new TranscriptCreateData(
                1L, "{\"pages\": []}",
                2023, "재학", null, null, null, null, null, null,
                0, BigDecimal.ZERO, 0, null,
                false, false, false, false, false,
                null, null, null, null, false));
    }

    private Transcript createFullTranscript() {
        return Transcript.create(new TranscriptCreateData(
                1L, "{\"pages\": []}",
                2023, "재학", "단일", 100L, null, null, null, null,
                80, new BigDecimal("3.95"), 4, "S1",
                true, false, false, false, true,
                true, 2, 1, null, false));
    }
}
