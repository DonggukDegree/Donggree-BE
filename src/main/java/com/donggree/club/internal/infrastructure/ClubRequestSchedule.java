package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubRequestRetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** 매일 새벽 보유 기간(1년)이 지난 요청자 연락처·관계를 비운다. 실패해도 다음 날 다시 비운다. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ClubRequestSchedule {

    private final ClubRequestRetentionService retentionService;

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void clearExpiredRequesterInfo() {
        retentionService.clearExpiredRequesterInfo();
    }
}
