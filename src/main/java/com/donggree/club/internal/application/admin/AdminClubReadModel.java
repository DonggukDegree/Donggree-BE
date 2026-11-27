package com.donggree.club.internal.application.admin;

import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 동아리 목록 한 개.
 *
 * @param editors   직접 수정 권한 회원(부여 순)
 * @param updatedAt 마지막 정보 등록·수정 시각
 */
public record AdminClubReadModel(ClubSummaryReadModel club, List<Editor> editors, LocalDateTime updatedAt) {

    /** @param name 실명. 이름이 없으면 null */
    public record Editor(Long memberId, String name, LocalDateTime grantedAt) {}
}
