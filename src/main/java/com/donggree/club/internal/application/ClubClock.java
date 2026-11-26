package com.donggree.club.internal.application;

import java.time.LocalDateTime;

/** 모집 상태·활동 확인 기준 시각. 클라이언트 시계와 무관하게 한국 시간으로 정한다. */
public interface ClubClock {

    LocalDateTime now();
}
