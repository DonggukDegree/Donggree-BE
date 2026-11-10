package com.donggree.support.internal.application.readmodel;

import com.donggree.support.internal.domain.enums.FaqTag;

/** FAQ 조회 결과. */
public record FaqReadModel(Long id, FaqTag tag, String title, String content) {}
