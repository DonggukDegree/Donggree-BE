package com.donggree.transcript.internal.application;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.ParsedCourse;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptParser;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF 업로드 및 성적표 관리를 담당하는 응용 서비스.
 * PDF 파싱, Transcript 생성/조회, 수강 이력 관리 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;
    private final CurriculumLookupService curriculumLookupService;
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;

    private final TranscriptParser parser = new TranscriptParser();

    /**
     * PDF 바이트 배열을 파싱하여 Transcript를 생성하고 저장한다.
     * 이미 성적표가 존재하면 기존 것을 소프트 삭제한 뒤 새로 생성한다.
     *
     * @param memberId 로그인한 회원 ID
     * @param pdfBytes 업로드된 PDF 파일 바이트 배열
     * @return 생성된 Transcript의 ID (= reportId)
     * @throws GeneralException PDF 파싱 실패 시
     */
    @Transactional
    public Long createTranscript(Long memberId, byte[] pdfBytes) {
        String pdfText = extractText(pdfBytes);
        ParsedTranscriptData parsedData = parseText(pdfText);
        String rawDataJson = buildRawDataJson(pdfText, parsedData);

        // 기존 성적표가 있으면 소프트 삭제
        transcriptRepository.findByMemberId(memberId)
                .ifPresent(existing -> existing.markAsDeleted(LocalDateTime.now()));

        Transcript transcript = Transcript.create(
                buildCreateData(memberId, rawDataJson, parsedData));

        // 파싱된 과목들을 CourseRecord로 추가 (curriculum 모듈에서 ID 조회/생성)
        for (ParsedCourse course : parsedData.courses()) {
            Long courseId = curriculumLookupService.findOrCreateCourseId(
                    course.courseCode(), course.courseName(), course.credits());
            Long areaTypeId = course.area().isEmpty()
                    ? null
                    : curriculumLookupService.findOrCreateAreaTypeId(course.area());

            transcript.addCourseRecord(
                    course.semester(),
                    CourseType.fromCategory(course.category()),
                    areaTypeId,
                    courseId,
                    Grade.fromValue(course.grade()),
                    course.retake()
            );
        }

        return transcriptRepository.save(transcript).getId();
    }

    private String extractText(byte[] pdfBytes) {
        try {
            return pdfTextExtractor.extract(pdfBytes);
        } catch (IOException e) {
            throw new GeneralException(TranscriptErrorCode.INVALID_PDF_FILE);
        }
    }

    private ParsedTranscriptData parseText(String pdfText) {
        return parser.parse(pdfText);
    }

    /**
     * 원문 텍스트와 파싱 결과를 합쳐 rawData JSON을 생성한다.
     * 원문을 보관하여 파싱 룰 변경 시 재처리가 가능하도록 한다.
     */
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

    /**
     * 파싱된 메타 정보를 TranscriptCreateData로 매핑한다.
     * 학과/부전공/복수전공 이름으로 Department ID를 조회하여 매핑한다.
     */
    private TranscriptCreateData buildCreateData(Long memberId, String rawDataJson,
                                                  ParsedTranscriptData parsedData) {
        Map<String, String> meta = parsedData.meta();

        return new TranscriptCreateData(
                memberId,
                rawDataJson,
                parseIntOrDefault(meta.get("교육과정 적용년도"), 0),
                meta.getOrDefault("학적상태", "재학"),
                meta.get("과정"),
                resolveMainDepartmentId(meta.get("대학"), meta.get("학과")),
                findDepartmentId(meta.get("부전공1")),
                findDepartmentId(meta.get("부전공2")),
                findDepartmentId(meta.get("복수1")),
                findDepartmentId(meta.get("복수2")),
                parseIntOrDefault(meta.get("총취득학점"), 0),
                parseBigDecimalOrDefault(meta.get("평점평균"), BigDecimal.ZERO),
                parseIntOrDefault(meta.get("이수학기"), 0),
                meta.get("레벨테스트(텝스)"),
                isYes(meta.get("공학인증심화대상")),
                isPresent(meta.get("전적대")),
                isYes(meta.get("선택적수료승인")),
                isYes(meta.get("글로벌인재트랙여부")),
                false,  // englishCourseTarget: 파싱 대상 아님
                toPassFail(meta.get("영어강의이수결과")),
                null,   // completedEnglishMajor: 파싱 대상 아님
                null,   // completedEnglishNonMajor: 파싱 대상 아님
                parseIntOrNull(meta.get("교직인적성합격횟수")),
                "합격".equals(meta.get("졸업논문심사"))
        );
    }

    // ====== curriculum 조회 헬퍼 ======

    /** 대학명 + 학과명으로 Department를 조회하거나 새로 생성한다. */
    private Long resolveMainDepartmentId(String collegeName, String departmentName) {
        if (departmentName == null || departmentName.isBlank()) return null;
        if (collegeName == null || collegeName.isBlank()) return null;
        return curriculumLookupService.findOrCreateDepartmentId(collegeName, departmentName);
    }

    /** 학과명으로 Department를 조회한다. 대학명 없이는 생성 불가이므로 조회만 시도. */
    private Long findDepartmentId(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) return null;
        return curriculumLookupService.findDepartmentIdByName(departmentName).orElse(null);
    }

    // ====== 메타 값 변환 헬퍼 ======

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

    /** "Y" 또는 값이 있으면 true, "N" 또는 null이면 false */
    private static boolean isYes(String value) {
        return value != null && !value.isBlank() && !"N".equals(value.trim());
    }

    /** 값이 존재하고 비어있지 않으면 true */
    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /** "PASS" → true, "FAIL" → false, 그 외 → null */
    private static Boolean toPassFail(String value) {
        if ("PASS".equals(value)) return true;
        if ("FAIL".equals(value)) return false;
        return null;
    }
}
