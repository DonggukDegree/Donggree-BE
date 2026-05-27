package com.donggree.transcript;

/**
 * 성적표가 생성될 때 발행되는 도메인 이벤트.
 * PDF에서 파싱한 학번·이름을 담아 user 모듈의 본인 인증에 활용한다.
 *
 * @param memberId  성적표 소유 회원 ID
 * @param studentId PDF에서 파싱한 학번
 * @param name      PDF에서 파싱한 성명
 */
public record TranscriptCreatedEvent(Long memberId, String studentId, String name) {
}
