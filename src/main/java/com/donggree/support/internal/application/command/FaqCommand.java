package com.donggree.support.internal.application.command;

import com.donggree.support.internal.domain.enums.FaqTag;

/** FAQ 등록·수정 입력. 등록과 전체 수정(PUT)이 같은 형태라 하나로 쓴다. */
public record FaqCommand(FaqTag tag, String title, String content) {}
