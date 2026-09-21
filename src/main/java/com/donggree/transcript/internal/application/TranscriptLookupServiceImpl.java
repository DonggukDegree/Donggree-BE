package com.donggree.transcript.internal.application;

import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranscriptLookupServiceImpl implements TranscriptLookupService {

    private final TranscriptRepository transcriptRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<TranscriptView> findById(Long transcriptId) {
        return transcriptRepository.findWithCourseRecordsById(transcriptId).map(this::toView);
    }

    @Override
    public Optional<TranscriptView> findByMemberId(Long memberId) {
        return transcriptRepository.findWithCourseRecordsByMemberId(memberId).map(this::toView);
    }

    private TranscriptView toView(Transcript t) {
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
                readDualMajor1ThesisStatus(t),
                t.isTransfer(),
                records);
    }

    /** 이미 보존한 원문 메타를 활용한다. 누락·미판정은 주전공 결과로 대체하거나 합격 처리하지 않는다. */
    private boolean readDualMajor1ThesisStatus(Transcript transcript) {
        if (transcript.getDualMajor1Id() == null || transcript.getRawData() == null) return false;
        try {
            return "합격"
                    .equals(objectMapper
                            .readTree(transcript.getRawData())
                            .path("meta")
                            .path("복수1졸업논문심사")
                            .asText("")
                            .trim());
        } catch (JsonProcessingException e) {
            // 성적표 원문에는 개인정보가 있으므로 예외 메시지나 JSON 원문은 로그에 남기지 않는다.
            log.warn("성적표 {}의 복수1 논문·시험 결과 메타 파싱 실패", transcript.getId());
            return false;
        }
    }

    private CourseRecordView toCourseRecordView(CourseRecord cr) {
        boolean passed = cr.getGrade() != null && cr.getGrade() != Grade.F && cr.getGrade() != Grade.NP;
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
}
