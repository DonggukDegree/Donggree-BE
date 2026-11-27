package com.donggree.club.internal.application.command;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDate;
import java.util.List;

/**
 * 동아리 신규 등록·정보 수정 입력. 입력 규칙은 도메인이 검증한다.
 *
 * @param imageCount       고른 사진 수(사진 저장 기능 전까지 비교 표시용)
 * @param requesterContact 승인 필요 요청의 확인 연락처. 바로 반영이면 무시
 * @param requesterRole    승인 필요 요청의 동아리와의 관계. 바로 반영이면 무시
 */
public record ClubSaveCommand(
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        String description,
        Integer generation,
        LocalDate activityStartDate,
        LocalDate activityEndDate,
        String roomLocation,
        Integer generationMemberCount,
        Integer totalMemberCount,
        LocalDate foundedDate,
        List<Officer> officers,
        List<Link> links,
        int imageCount,
        String requesterContact,
        ClubRequesterRole requesterRole) {

    public record Officer(String role, String department, String name) {}

    public record Link(String label, String url) {}
}
