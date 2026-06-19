package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.dto.GraduationRuleUpsertCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 졸업 규칙 배치 업서트 요청. 등록·수정 항목을 한 번에 담아 전송한다.
 */
public record GraduationRuleBatchRequest(
        @NotEmpty(message = "items는 비어 있을 수 없습니다.") @Valid List<GraduationRuleUpsertItem> items) {

    public List<GraduationRuleUpsertCommand> toCommands() {
        return items.stream().map(GraduationRuleUpsertItem::toCommand).toList();
    }
}
