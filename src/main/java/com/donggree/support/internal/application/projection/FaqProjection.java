package com.donggree.support.internal.application.projection;

import com.donggree.support.internal.domain.enums.FaqTag;

/** FAQ 조회 결과. */
public record FaqProjection(Long id, FaqTag tag, String title, String content) {}
