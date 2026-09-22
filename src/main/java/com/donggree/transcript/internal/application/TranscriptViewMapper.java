package com.donggree.transcript.internal.application;

import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.ParsedCourse;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 저장 성적표와 임시 파싱 결과를 동일한 판정 입력으로 변환한다. 개인정보는 판정 뷰에 포함하지 않는다. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TranscriptViewMapper {
    private final ObjectMapper objectMapper;

    public TranscriptView toView(Transcript t) {
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
                readDualMajor1ThesisStatus(t.getId(), t.getDualMajor1Id(), t.getRawData()),
                t.isTransfer(),
                records);
    }

    /** 이미 보존한 원문 메타를 활용한다. 누락·미판정은 주전공 결과로 대체하거나 합격 처리하지 않는다. */
    private boolean readDualMajor1ThesisStatus(Long transcriptId, Long dualMajor1Id, String rawData) {
        if (dualMajor1Id == null || rawData == null) return false;
        try {
            return "합격"
                    .equals(objectMapper
                            .readTree(rawData)
                            .path("meta")
                            .path("복수1졸업논문심사")
                            .asText("")
                            .trim());
        } catch (JsonProcessingException e) {
            // 성적표 원문에는 개인정보가 있으므로 예외 메시지나 JSON 원문은 로그에 남기지 않는다.
            log.warn("성적표 {}의 복수1 논문·시험 결과 메타 파싱 실패", transcriptId);
            return false;
        }
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
                readDualMajor1ThesisStatus(null, data.dualMajor1Id(), data.rawData()),
                data.transfer(),
                records);
    }

    private boolean isPassed(Grade grade) {
        return grade != null && grade != Grade.F && grade != Grade.NP;
    }
}
