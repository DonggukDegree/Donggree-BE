package com.donggree.transcript.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.TranscriptCreatedEvent;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawCourseRecord;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawMeta;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawSemesterGroup;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptParser;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF 업로드 및 성적표 관리를 담당하는 응용 서비스.
 * 트랜잭션 경계와 도메인 객체 조립만 담당한다.
 * 커리큘럼 모듈 의존(ID 조회/이름 해소)은 컨트롤러에서 처리한 뒤 이 서비스에 전달한다.
 */
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

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
            Long memberId, String rawDataJson, ParsedTranscriptData parsedData,
            Long deptId, Long sub1Id, Long sub2Id, Long dual1Id, Long dual2Id) {
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
                parseIntOrNull(meta.get("교직인적성합격횟수")),
                "합격".equals(meta.get("졸업논문심사"))
        );
    }

    /**
     * 사전 resolve된 데이터로 Transcript를 생성하고 저장한다.
     * 기존 성적표가 있으면 소프트 삭제 후 새로 생성한다.
     *
     * @param createData    메타 정보 및 resolve된 학과 ID
     * @param courses       resolve된 과목 ID와 영역 ID를 포함한 수강 이력 목록
     * @return 생성된 Transcript의 ID
     */
    @Transactional
    public Long createTranscript(TranscriptCreateData createData, List<CourseRecordCreateData> courses,
                                  String pdfStudentId, String pdfName) {
        transcriptRepository.findByMemberId(createData.memberId())
                .ifPresent(existing -> {
                    existing.markAsDeleted(LocalDateTime.now());
                    transcriptRepository.flush();
                });

        Transcript transcript = Transcript.create(createData);

        for (CourseRecordCreateData course : courses) {
            transcript.addCourseRecord(
                    course.semester(), course.courseType(), course.areaName(),
                    course.courseCode(), course.courseName(), course.credits(),
                    course.grade(), course.retake()
            );
        }

        Long savedId = transcriptRepository.save(transcript).getId();
        eventPublisher.publishEvent(new TranscriptCreatedEvent(createData.memberId(), pdfStudentId, pdfName));
        return savedId;
    }

    /**
     * 기존 성적표에 수강 이력을 수동으로 추가한다.
     * 성적표가 없으면 예외를 던진다.
     *
     * @param memberId 로그인한 회원 ID
     * @param courses  추가할 수강 이력 목록
     * @return 새로 생성된 CourseRecord ID 목록
     */
    @Transactional
    public List<Long> addCourseRecords(Long memberId, List<CourseRecordCreateData> courses) {
        Transcript transcript = transcriptRepository.findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.TRANSCRIPT_NOT_FOUND));

        List<CourseRecord> added = new ArrayList<>();
        for (CourseRecordCreateData course : courses) {
            added.add(transcript.addCourseRecord(
                    course.semester(), course.courseType(), course.areaName(),
                    course.courseCode(), course.courseName(), course.credits(),
                    course.grade(), course.retake()
            ));
        }

        transcriptRepository.saveAndFlush(transcript);
        return added.stream().map(CourseRecord::getId).toList();
    }

    /**
     * 로그인한 회원의 성적표 데이터를 전체 조회한다.
     * 수강 이력은 학기 오름차순으로 그룹핑하여 반환한다.
     * 커리큘럼 이름 해소 없이 ID만 담은 {@link TranscriptQueryResult}를 반환하며,
     * 이름 변환은 컨트롤러에서 수행한다.
     *
     * @param memberId 로그인한 회원 ID
     */
    @Transactional(readOnly = true)
    public TranscriptQueryResult getTranscriptRawReport(Long memberId) {
        Transcript transcript = transcriptRepository.findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.TRANSCRIPT_NOT_FOUND));

        List<RawSemesterGroup> semesterGroups = transcript.getCourseRecords().stream()
                .sorted((a, b) -> a.getSemester().compareTo(b.getSemester()))
                .collect(Collectors.groupingBy(CourseRecord::getSemester, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new RawSemesterGroup(
                        e.getKey(),
                        e.getValue().stream().map(r -> new RawCourseRecord(
                                r.getId(),
                                r.getCourseCode(),
                                r.getCourseName(),
                                r.getCredits(),
                                r.getAreaName(),
                                r.getCourseType() != null ? r.getCourseType().name() : null,
                                r.getGrade().getValue(),
                                r.isRetake()
                        )).toList()
                ))
                .toList();

        RawMeta meta = new RawMeta(
                transcript.getId(),
                transcript.getAdmissionYear(),
                transcript.getDepartmentId(),
                transcript.getSubMajor1Id(),
                transcript.getSubMajor2Id(),
                transcript.getDualMajor1Id(),
                transcript.getDualMajor2Id(),
                transcript.getAcademicStatus(),
                transcript.getTotalCredits(),
                transcript.getGpa(),
                transcript.getCompletedSemesters()
        );

        return new TranscriptQueryResult(meta, semesterGroups);
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
            throw new GeneralException(TranscriptErrorCode.PDF_PARSING_FAILED);
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
