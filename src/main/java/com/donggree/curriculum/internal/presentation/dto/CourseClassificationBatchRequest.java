package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.dto.CourseClassificationUpsertCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 과목 분류 배치 업서트 요청. 등록·수정 항목을 한 번에 담아 전송한다.
 */
public record CourseClassificationBatchRequest(
        @NotEmpty(message = "items는 비어 있을 수 없습니다.") @Valid List<CourseClassificationUpsertItem> items) {

    public List<CourseClassificationUpsertCommand> toCommands() {
        return items.stream().map(CourseClassificationUpsertItem::toCommand).toList();
    }
}
