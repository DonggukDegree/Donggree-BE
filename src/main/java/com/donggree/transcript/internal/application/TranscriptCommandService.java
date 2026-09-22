package com.donggree.transcript.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.internal.application.command.CourseRecordCreateCommand;
import com.donggree.transcript.internal.application.command.TranscriptCreateResult;
import com.donggree.transcript.internal.application.command.TranscriptParseResult;
import com.donggree.transcript.internal.application.command.TranscriptUpdateResult;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.CourseRecordData;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.user.MemberIdentityService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF 업로드 및 성적표 변경(Command)을 담당하는 응용 서비스.
 * 트랜잭션 경계와 도메인 객체 조립만 담당한다.
 * 커리큘럼 모듈 의존(ID 조회/이름 해소)은 컨트롤러에서 처리한 뒤 이 서비스에 전달한다.
 */
@Service
@RequiredArgsConstructor
public class TranscriptCommandService {

    private final TranscriptRepository transcriptRepository;
    private final TranscriptPdfReader pdfReader;
    private final MemberIdentityService memberIdentityService;

    /** 일반 업로드와 관리자 미리보기의 동일한 파싱·변환 경로를 사용한다. */
    public TranscriptParseResult parseTranscript(byte[] pdfBytes) {
        return pdfReader.parseTranscript(pdfBytes);
    }

    public TranscriptCreateData buildCreateData(
            Long memberId,
            String rawDataJson,
            ParsedTranscriptData parsedData,
            Long deptId,
            Long sub1Id,
            Long sub2Id,
            Long dual1Id,
            Long dual2Id) {
        return pdfReader.buildCreateData(memberId, rawDataJson, parsedData, deptId, sub1Id, sub2Id, dual1Id, dual2Id);
    }

    /**
     * 사전 resolve된 데이터로 Transcript를 생성하고 저장한다.
     * 기존 성적표가 있으면 소프트 삭제 후 새로 생성한다.
     *
     * @param createData    메타 정보 및 resolve된 학과 ID
     * @param courses       resolve된 과목 ID와 영역 ID를 포함한 수강 이력 목록
     * @return 총취득학점과 수강 이력 학점 합, 그 차이를 담은 등록 결과
     */
    @Transactional
    public TranscriptCreateResult createTranscript(
            TranscriptCreateData createData,
            List<CourseRecordCreateCommand> courses,
            String pdfStudentId,
            String pdfName) {
        transcriptRepository.findByMemberId(createData.memberId()).ifPresent(existing -> {
            existing.markAsDeleted(LocalDateTime.now());
            transcriptRepository.flush();
        });

        Transcript transcript = Transcript.create(createData);

        for (CourseRecordCreateCommand course : courses) {
            transcript.addCourseRecord(
                    course.semester(),
                    course.courseTypeName(),
                    course.areaName(),
                    course.courseCode(),
                    course.courseName(),
                    course.credits(),
                    course.grade(),
                    course.retake());
        }

        Transcript saved = transcriptRepository.save(transcript);
        memberIdentityService.verifyIdentityIfMatch(createData.memberId(), pdfStudentId, pdfName);

        // recordedCredits()와 creditGap()을 따로 호출하면 학점 합산 스트림이 중복 수행되므로 한 번만 계산해 재사용한다.
        int recordedCredits = saved.recordedCredits();
        return new TranscriptCreateResult(
                saved.getTotalCredits(), recordedCredits, saved.getTotalCredits() - recordedCredits);
    }

    /**
     * 기존 성적표의 수강 이력을 전송된 목록으로 통째 치환한다.
     * 기존 이력 중 목록에 없는 것은 삭제되고, 새 항목은 추가되므로 수정·삭제·추가를 한 번에 반영한다.
     * 치환 후 총취득학점과 평점 평균(GPA)을 재계산한다. 성적표가 없으면 예외를 던진다.
     *
     * @param memberId 로그인한 회원 ID
     * @param courses  변경 후의 전체 수강 이력 목록
     * @return 재계산된 총취득학점·GPA와 학기 오름차순으로 그룹핑한 전체 수강 이력
     */
    @Transactional
    public TranscriptUpdateResult replaceCourseRecords(Long memberId, List<CourseRecordCreateCommand> courses) {
        Transcript transcript = transcriptRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.TRANSCRIPT_NOT_FOUND));

        List<CourseRecordData> newRecords = courses.stream()
                .map(c -> new CourseRecordData(
                        c.semester(),
                        c.courseTypeName(),
                        c.areaName(),
                        c.courseCode(),
                        c.courseName(),
                        c.credits(),
                        c.grade(),
                        c.retake()))
                .toList();

        transcript.replaceCourseRecords(newRecords);
        transcriptRepository.saveAndFlush(transcript);

        return new TranscriptUpdateResult(
                transcript.getTotalCredits(), transcript.getGpa(), TranscriptRecordGrouper.groupBySemester(transcript));
    }
}
