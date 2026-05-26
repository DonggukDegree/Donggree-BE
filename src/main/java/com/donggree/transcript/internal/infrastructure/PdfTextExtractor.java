package com.donggree.transcript.internal.infrastructure;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * PDFBox를 사용하여 PDF 바이트 배열에서 텍스트를 추출하는 인프라스트럭처 서비스.
 * 위치 기반 정렬을 활성화하여 원본 레이아웃 순서를 유지한다.
 */
@Component
public class PdfTextExtractor {

    /**
     * PDF 바이트 배열에서 텍스트를 추출한다.
     *
     * @param pdfBytes PDF 파일의 바이트 배열
     * @return 추출된 텍스트
     * @throws IOException PDF 로딩 또는 텍스트 추출 실패 시
     */
    public String extract(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }
}
