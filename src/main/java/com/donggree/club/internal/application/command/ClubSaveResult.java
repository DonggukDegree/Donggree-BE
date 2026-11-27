package com.donggree.club.internal.application.command;

/**
 * 저장 결과. 직접 수정 권한이 있으면 바로 반영(DIRECT), 아니면 승인 요청(PENDING)으로 들어간다.
 *
 * @param clubId 대상 동아리. 신규 등록 요청은 승인 전이라 null
 */
public record ClubSaveResult(Applied applied, Long clubId) {

    public enum Applied {
        DIRECT,
        PENDING
    }

    public static ClubSaveResult direct(Long clubId) {
        return new ClubSaveResult(Applied.DIRECT, clubId);
    }

    public static ClubSaveResult pending(Long clubId) {
        return new ClubSaveResult(Applied.PENDING, clubId);
    }
}
