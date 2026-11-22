package com.donggree.mystery.internal.infrastructure;

import java.time.*;
import org.springframework.stereotype.Component;

/** 게임 날짜는 클라이언트 시계와 무관하게 한국 표준시로 결정한다. */
@Component
public class MysteryClock implements com.donggree.mystery.internal.application.MysteryCalendar {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public Instant now() {
        return Instant.now();
    }

    public LocalDate today() {
        return now().atZone(ZONE).toLocalDate();
    }
}
