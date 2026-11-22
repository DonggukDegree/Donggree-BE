package com.donggree.mystery.internal.application;

import java.time.LocalDate;

/** 게임 날짜 조회 포트. 서버 시계 구현은 인프라에서 제공한다. */
public interface MysteryCalendar {
    LocalDate today();
}
