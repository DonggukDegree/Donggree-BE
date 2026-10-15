package com.donggree.transcript.internal.application.pdf;

import java.io.IOException;

/** PDF 라이브러리와 분리된 업로드·미리보기의 텍스트 추출 포트. */
public interface PdfTextReader {
    String extract(byte[] pdfBytes) throws IOException;
}
