package com.donggree.support.internal.presentation.dto;

import com.donggree.support.internal.application.projection.FaqProjection;
import com.donggree.support.internal.domain.enums.FaqTag;

/** FAQ 조회 응답. 목록 한 건이 곧 아코디언 한 줄이므로 본문까지 함께 내려준다. */
public record FaqResponse(Long id, FaqTag tag, String title, String content) {
    public static FaqResponse from(FaqProjection p) {
        return new FaqResponse(p.id(), p.tag(), p.title(), p.content());
    }
}
