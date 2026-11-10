package com.donggree.transcript.internal.application.pdf;

import com.donggree.transcript.internal.application.command.ParsedTranscriptData;

/** 추출된 텍스트를 원시 성적표 결과로 변환하는 포트. PDF 형식 해석은 인프라에서 구현한다. */
public interface TranscriptTextParser {
    ParsedTranscriptData parse(String text);
}
