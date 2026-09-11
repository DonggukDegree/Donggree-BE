package com.donggree.transcript.internal.application;

import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.Grade;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranscriptLookupServiceImpl implements TranscriptLookupService {

    private final TranscriptRepository transcriptRepository;

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
                t.getTotalCredits(),
                t.getGpa(),
                t.getEnglishLevel(),
                t.isEnglishCourseTarget(),
                t.getCompletedEnglishResult(),
                t.isThesisStatus(),
                records);
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
