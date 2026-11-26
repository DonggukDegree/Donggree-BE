package com.donggree.club.event;

/**
 * 관리자가 회원의 동아리 요청을 승인했다. 승인과 같은 트랜잭션에서 발행하며, 포인트 모듈이 커밋 전에 적립한다.
 *
 * @param memberId            요청 회원 ID
 * @param clubId              대상 동아리 ID(신규 등록이면 승인으로 만든 동아리)
 * @param firstRegistration   이 승인으로 동아리가 처음 정식 등록됐는지(신규 등록 승인 또는 임시 등록 동아리의 첫 승인)
 * @param recruitedGeneration 이 동아리가 처음 모집하는 기수의 새 모집 건이면 그 기수, 아니면 null
 * @param updatedGeneration   정보 수정으로 현재 기수가 올라갔으면 새 기수, 아니면 null
 */
public record ClubRequestApprovedEvent(
        Long memberId,
        Long clubId,
        boolean firstRegistration,
        Integer recruitedGeneration,
        Integer updatedGeneration) {}
