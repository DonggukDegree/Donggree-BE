package com.donggree.transcript.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawMeta;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성적표 조회(Query) 응용 서비스.
 * 상태를 변경하지 않는 읽기 유스케이스만 담당한다.
 * 커리큘럼 이름 해소 없이 ID만 담은 프로젝션을 반환하며, 이름 변환은 컨트롤러에서 수행한다.
 */
@Service
@RequiredArgsConstructor
public class TranscriptQueryService {

    private final TranscriptRepository transcriptRepository;

    /**
     * 로그인한 회원의 성적표 데이터를 전체 조회한다.
     * 수강 이력은 학기 오름차순으로 그룹핑하여 반환한다.
     *
     * @param memberId 로그인한 회원 ID
     */
    @Transactional(readOnly = true)
    public TranscriptReportProjection getTranscriptRawReport(Long memberId) {
        Transcript transcript = transcriptRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.TRANSCRIPT_NOT_FOUND));

        RawMeta meta = new RawMeta(
                transcript.getAdmissionYear(),
                transcript.getDepartmentId(),
                transcript.getSubMajor1Id(),
                transcript.getSubMajor2Id(),
                transcript.getDualMajor1Id(),
                transcript.getDualMajor2Id(),
                transcript.getAcademicStatus(),
                transcript.getTotalCredits(),
                transcript.getGpa(),
                transcript.calculateMajorGpa(),
                transcript.calculateDualMajor1Gpa(),
                transcript.getCompletedSemesters(),
                transcript.getCreatedAt(),
                transcript.getUpdatedAt());

        return new TranscriptReportProjection(meta, TranscriptRecordGrouper.groupBySemester(transcript));
    }
}
