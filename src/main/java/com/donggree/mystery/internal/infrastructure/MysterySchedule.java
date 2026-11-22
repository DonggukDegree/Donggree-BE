package com.donggree.mystery.internal.infrastructure;

import com.donggree.mystery.internal.application.MysteryCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

/** 미리 출제하되, 실패해도 다음 조회에서 같은 날짜를 멱등 생성한다. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MysterySchedule {
    private final MysteryCaseService cases;
    private final MysteryClock clock;

    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Seoul")
    public void prepareTomorrow() {
        cases.getOrCreate(clock.today().plusDays(1));
    }
}
