package com.donggree.transcript.internal.application.command;

/**
 * 성적표 등록 결과.
 * 등록된 성적표의 총취득학점, 파싱된 수강 이력 학점 합, 그 차이(creditGap)를 담는다.
 * creditGap이 0이 아니면 PDF 파싱 결과와 총취득학점이 불일치함을 의미한다.
 */
public record TranscriptCreateResult(int totalCredits, int recordedCredits, int creditGap) {}
