package com.donggree.transcript.internal.application;

import com.donggree.global.logging.RequestDiagnostics;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.ParsedCourse;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 저장 성적표와 임시 파싱 결과를 동일한 판정 입력으로 변환한다. 개인정보는 판정 뷰에 포함하지 않는다. */
@Service
@RequiredArgsConstructor
public class TranscriptViewMapper {
    private final ObjectMapper objectMapper;

    public TranscriptView toView(Transcript t) {
        JsonNode meta = readMeta(t.getId(), t.getRawData());
        List<CourseRecordView> records =
                t.getCourseRecords().stream().map(this::toCourseRecordView).toList();
        return new TranscriptView(
                t.getId(),
                t.getMemberId(),
                t.getDepartmentId(),
                t.getDualMajor1Id(),
                t.getDualMajor2Id(),
                t.getSubMajor1Id(),
                t.getSubMajor2Id(),
                t.getAdmissionYear(),
                t.getStudentType(),
                t.isEngineeringCertified(),
                t.getTotalCredits(),
                t.getGpa(),
                t.getEnglishLevel(),
                t.isEnglishCourseTarget(),
                t.getCompletedEnglishResult(),
                t.getEnglishPassResult(),
                t.isThesisStatus(),
                readDualMajor1ThesisStatus(t.getDualMajor1Id(), meta),
                t.isTransfer(),
                hasName(meta, "복수1") || hasName(meta, "복수2"),
                unresolved(meta, t.getDualMajor1Id(), t.getDualMajor2Id(), t.getSubMajor1Id(), t.getSubMajor2Id()),
                records,
                t.calculateMajorGpa(),
                t.getTeachingAptitudeCount());
    }

    /** 이미 보존한 원문 메타를 활용한다. 누락·미판정은 주전공 결과로 대체하거나 합격 처리하지 않는다. */
    private boolean readDualMajor1ThesisStatus(Long dualMajor1Id, JsonNode meta) {
        return dualMajor1Id != null
                && "합격".equals(meta.path("복수1졸업논문심사").asText("").trim());
    }

    // 학업정보 조회에서도 동일한 원문 파싱과 개인정보 비노출 정책을 사용한다.
    JsonNode readMeta(Long transcriptId, String rawData) {
        if (rawData == null || rawData.isBlank()) return MissingNode.getInstance();
        try {
            return objectMapper.readTree(rawData).path("meta");
        } catch (JsonProcessingException e) {
            // 성적표 원문에는 개인정보가 있으므로 예외 메시지나 JSON 원문은 로그에 남기지 않는다.
            RequestDiagnostics.fallback("transcript_meta_parse", e);
            return MissingNode.getInstance();
        }
    }

    private boolean hasName(JsonNode meta, String key) {
        return !meta.path(key).asText("").isBlank();
    }

    private boolean unresolved(JsonNode meta, Long dual1, Long dual2, Long minor1, Long minor2) {
        return (dual1 == null && hasName(meta, "복수1"))
                || (dual2 == null && hasName(meta, "복수2"))
                || (minor1 == null && hasName(meta, "부전공1"))
                || (minor2 == null && hasName(meta, "부전공2"));
    }

    private CourseRecordView toCourseRecordView(CourseRecord cr) {
        boolean passed = isPassed(cr.getGrade());
        return new CourseRecordView(
                cr.getSemester(),
                cr.getCourseCode(),
                cr.getCourseTypeName(),
                cr.getAreaName(),
                cr.getCourseName(),
                cr.getCredits(),
                passed,
                cr.isRetake());
    }
    /** 미저장 성적표에는 회원·성적표 ID를 부여하지 않는다. */
    public TranscriptView toPreview(TranscriptCreateData data, List<ParsedCourse> courses) {
        JsonNode meta = readMeta(null, data.rawData());
        List<CourseRecordView> records = courses.stream()
                .map(course -> {
                    Grade grade = Grade.fromValue(course.grade());
                    return new CourseRecordView(
                            course.semester(),
                            course.courseCode(),
                            course.category(),
                            course.area() == null || course.area().isBlank() ? null : course.area(),
                            course.courseName(),
                            course.credits(),
                            isPassed(grade),
                            course.retake());
                })
                .toList();
        return new TranscriptView(
                null,
                null,
                data.departmentId(),
                data.dualMajor1Id(),
                data.dualMajor2Id(),
                data.subMajor1Id(),
                data.subMajor2Id(),
                data.admissionYear(),
                data.studentType(),
                data.engineeringCertified(),
                data.totalCredits(),
                data.gpa(),
                data.englishLevel(),
                data.englishCourseTarget(),
                data.completedEnglishResult(),
                data.englishPassResult(),
                data.thesisStatus(),
                readDualMajor1ThesisStatus(data.dualMajor1Id(), meta),
                data.transfer(),
                hasName(meta, "복수1") || hasName(meta, "복수2"),
                unresolved(meta, data.dualMajor1Id(), data.dualMajor2Id(), data.subMajor1Id(), data.subMajor2Id()),
                records,
                calculateMajorGpa(courses),
                parseTeachingAptitudeCount(meta));
    }

    private Integer parseTeachingAptitudeCount(JsonNode meta) {
        String value = meta.path("교직인적성합격횟수").asText(null);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 미저장 미리보기도 저장 성적표와 같은 전공·전필 평점 산식을 사용한다. */
    private BigDecimal calculateMajorGpa(List<ParsedCourse> courses) {
        int denominator = 0;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (ParsedCourse course : courses) {
            if (!("전공".equals(course.category()) || "전필".equals(course.category()))) continue;
            Grade grade = Grade.fromValue(course.grade());
            if (grade == Grade.P || grade == Grade.NP) continue;
            denominator += course.credits();
            weightedSum = weightedSum.add(grade.getGradePoint().multiply(BigDecimal.valueOf(course.credits())));
        }
        return denominator == 0 ? null : weightedSum.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private boolean isPassed(Grade grade) {
        return grade != null && grade != Grade.F && grade != Grade.NP;
    }
}
