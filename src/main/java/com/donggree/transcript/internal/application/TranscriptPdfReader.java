package com.donggree.transcript.internal.application;

import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.internal.application.command.TranscriptParseResult;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptParser;
import com.donggree.transcript.internal.infrastructure.PdfTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 일반 업로드와 일회성 미리보기가 공유하는 저장·본인확인 없는 PDF 파싱 및 메타 변환 서비스. */
@Service
@RequiredArgsConstructor
public class TranscriptPdfReader {
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;
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
                parsedData.admissionYear(),
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
                parsedData.engineeringCertified(),
                isPresent(meta.get("전적대")),
                isYes(meta.get("선택적수료승인")),
                isYes(meta.get("글로벌인재트랙여부")),
                meta.get("영어강의이수대상") != null,
                toPassFail(meta.get("영어강의이수결과")),
                toPassFail(meta.get("영어패스제결과")),
                parseIntOrNull(meta.get("교직인적성합격횟수")),
                "합격".equals(meta.get("졸업논문심사")));
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
