package com.donggree.transcript.internal.application;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.transcript.TranscriptPreviewService;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 저장소·회원 서비스에 의존하지 않는 일회성 PDF 미리보기 입력 생성. */
@Service
@RequiredArgsConstructor
public class TranscriptPreviewServiceImpl implements TranscriptPreviewService {
    private final TranscriptPdfReader pdfReader;
    private final TranscriptViewMapper viewMapper;
    private final CurriculumLookupService curriculumLookupService;

    @Override
    public TranscriptView preview(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new GeneralException(TranscriptErrorCode.PDF_FILE_REQUIRED);
        }
        var parsed = pdfReader.parseTranscript(pdfBytes);
        Map<String, String> meta = parsed.parsedData().meta();
        // 이름·학번·학적상태·본인확인은 검사하지 않는다. 졸업 판정에 필요한 학사 정보만 확인한다.
        for (String key : new String[] {"학과", "교육과정 적용년도", "총취득학점", "평점평균"}) {
            if (meta.get(key) == null || meta.get(key).isBlank()) {
                throw new GeneralException(TranscriptErrorCode.PDF_PARSING_FAILED);
            }
        }
        Long departmentId = curriculumLookupService
                .findDepartmentIdByName(meta.get("학과"))
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.DEPARTMENT_NOT_FOUND));
        var data = pdfReader.buildCreateData(
                null,
                parsed.rawDataJson(),
                parsed.parsedData(),
                departmentId,
                findDepartment(meta.get("부전공1")),
                findDepartment(meta.get("부전공2")),
                findDepartment(meta.get("복수1")),
                findDepartment(meta.get("복수2")));
        return viewMapper.toPreview(data, parsed.parsedData().courses());
    }

    private Long findDepartment(String name) {
        if (name == null || name.isBlank()) return null;
        return curriculumLookupService.findDepartmentIdByName(name).orElse(null);
    }
}
