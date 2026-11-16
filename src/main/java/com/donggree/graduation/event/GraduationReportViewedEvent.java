package com.donggree.graduation.event;

/**
 * 회원이 자기 졸업 리포트 요약을 조회했다. 조회마다 동기로 발행하며 저장되지 않는다(트랜잭션 이벤트 아님).
 * 리스너는 실패해도 조회 응답을 막지 않아야 한다.
 */
public record GraduationReportViewedEvent(Long memberId) {}
