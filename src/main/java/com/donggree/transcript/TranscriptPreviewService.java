package com.donggree.transcript;

/** 관리자 미리보기용 공개 파싱 인터페이스. 저장·회원 본인확인 없이 판정에 필요한 정보만 반환한다. */
public interface TranscriptPreviewService {
    TranscriptView preview(byte[] pdfBytes);
}
