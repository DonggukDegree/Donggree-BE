package com.donggree.transcript.internal.presentation.dto;

/**
 * 성적표 생성 응답 DTO.
 * 생성된 성적표의 ID를 reportId로 반환한다.
 *
 * @param reportId 생성된 성적표 ID (= 학업 리포트 조회 시 사용하는 ID)
 */
public record TranscriptCreateResponse(Long reportId) {
}
