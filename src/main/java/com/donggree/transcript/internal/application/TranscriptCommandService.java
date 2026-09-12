package com.donggree.transcript.internal.application;

import com.donggree.global.apiPayload.code.GeneralErrorCode;
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
import com.donggree.transcript.internal.domain.TranscriptParser;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.donggree.user.MemberIdentityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;
    private final MemberIdentityService memberIdentityService;

    private final TranscriptParser parser = new TranscriptParser();

    /**
     * PDF를 파싱하여 결과와 원문 JSON을 반환한다.
     * 커리큘럼 ID 조회는 포함하지 않으며, 컨트롤러에서 ID를 resolve한 후 createTranscript()를 호출해야 한다.
     */
    public TranscriptParseResult parseTranscript(byte[] pdfBytes) {
        String pdfText = extractText(pdfBytes);
        ParsedTranscriptData parsedData = parser.parse(pdfText);
        String rawDataJson = buildRawDataJson(pdfText, parsedData);
        return new TranscriptParseResult(parsedData, rawDataJson);
    }

    /**
     * 파싱 결과와 사전 resolve된 학과 ID를 기반으로 TranscriptCreateData를 생성한다.
     * 메타 필드 변환 로직(parseIntOrDefault 등)을 캡슐화한다.
     */
    public TranscriptCreateData buildCreateData(
            Long memberId,
            String rawDataJson,
            ParsedTranscriptData parsedData,
            Long deptId,
            Long sub1Id,
            Long sub2Id,
            Long dual1Id,
            Long dual2Id) {
        Map<String, String> meta = parsedData.meta();
        return new TranscriptCreateData(
                memberId,
                rawDataJson,
                parseIntOrDefault(meta.get("교육과정 적용년도"), 0),
                meta.getOrDefault("학적상태", "재학"),
                meta.get("과정"),
                deptId,
                sub1Id,
                sub2Id,
                dual1Id,
                dual2Id,
                parseIntOrDefault(meta.get("총취득학점"), 0),
                parseBigDecimalOrDefault(meta.get("평점평균"), BigDecimal.ZERO),
                parseIntOrDefault(meta.get("이수학기"), 0),
                meta.get("레벨테스트(텝스)"),
                isYes(meta.get("공학인증심화대상")),
                isPresent(meta.get("전적대")),
                isYes(meta.get("선택적수료승인")),
                isYes(meta.get("글로벌인재트랙여부")),
                meta.get("영어강의이수대상") != null,
                toPassFail(meta.get("영어강의이수결과")),
                toPassFail(meta.get("영어패스제결과")),
                parseIntOrNull(meta.get("교직인적성합격횟수")),
                "합격".equals(meta.get("졸업논문심사")));
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

    // ====== 내부 헬퍼 ======

    private String extractText(byte[] pdfBytes) {
        try {
            return pdfTextExtractor.extract(pdfBytes);
        } catch (IOException e) {
            throw new GeneralException(TranscriptErrorCode.INVALID_PDF_FILE);
        }
    }

    private String buildRawDataJson(String pdfText, ParsedTranscriptData parsedData) {
        try {
            Map<String, Object> rawDataMap = new LinkedHashMap<>();
            rawDataMap.put("rawText", pdfText);
            rawDataMap.put("meta", parsedData.meta());
            rawDataMap.put("courses", parsedData.courses());
            return objectMapper.writeValueAsString(rawDataMap);
        } catch (JsonProcessingException e) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimalOrDefault(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isYes(String value) {
        return value != null && !value.isBlank() && !"N".equals(value.trim());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static Boolean toPassFail(String value) {
        if ("PASS".equals(value)) return true;
        if ("FAIL".equals(value)) return false;
        return null;
    }
}
