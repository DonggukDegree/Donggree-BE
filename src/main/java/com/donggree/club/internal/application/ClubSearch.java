package com.donggree.club.internal.application;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.util.Set;

/**
 * 목록 조회 조건. 필터 종류끼리는 모두 만족해야 하고, 같은 필터 안에서는 하나라도 해당하면 통과한다. 비운 필터는 전체다.
 *
 * @param ids 이 ID의 동아리로만 좁힌다(찜 모아보기). null이면 전체
 */
public record ClubSearch(
        Set<ClubAffiliation> affiliations,
        Set<ClubField> fields,
        Set<RecruitmentStatus> recruitmentStatuses,
        ClubSort sort,
        Set<Long> ids) {

    public ClubSearch {
        affiliations = affiliations == null ? Set.of() : Set.copyOf(affiliations);
        fields = fields == null ? Set.of() : Set.copyOf(fields);
        recruitmentStatuses = recruitmentStatuses == null ? Set.of() : Set.copyOf(recruitmentStatuses);
        sort = sort == null ? ClubSort.NAME : sort;
        ids = ids == null ? null : Set.copyOf(ids);
    }
}
