package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.projection.RequirementSetProjection;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import java.util.List;

/**
 * 졸업 요건 세트 상세 조회 응답. 연결된 졸업 규칙은 ID 목록으로 내려준다.
 */
public record RequirementSetResponse(
        Long id,
        Long departmentId,
        String departmentName,
        int yearStart,
        int yearEnd,
        RequirementTrack track,
        int version,
        String description,
        String sheetImageUrl,
        boolean active,
        List<Long> graduationRuleIds) {
    public static RequirementSetResponse from(RequirementSetProjection p) {
        return new RequirementSetResponse(
                p.id(),
                p.departmentId(),
                p.departmentName(),
                p.yearStart(),
                p.yearEnd(),
                p.track(),
                p.version(),
                p.description(),
                p.sheetImageUrl(),
                p.active(),
                p.graduationRuleIds());
    }
}
