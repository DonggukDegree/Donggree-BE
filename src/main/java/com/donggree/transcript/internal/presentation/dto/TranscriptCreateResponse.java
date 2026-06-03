package com.donggree.transcript.internal.presentation.dto;

/**
 * PUT /api/users/me/reports 응답 DTO.
 * 등록된 성적표의 총취득학점과 파싱된 수강 이력 학점 합, 그 차이(creditGap)를 담는다.
 * creditGap이 0이 아니면 PDF 파싱 결과와 총취득학점이 불일치함을 의미하며,
 * 프론트엔드는 이를 보고 사용자에게 수강 이력 추가를 안내할 수 있다.
 * (양수: 이수 이력 추가 필요, 음수: 재수강·P/F 등으로 과목 학점 합이 더 많음)
 */
public record TranscriptCreateResponse(int totalCredits, int recordedCredits, int creditGap) {}
