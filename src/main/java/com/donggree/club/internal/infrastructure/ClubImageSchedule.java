package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubImageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** 매일 새벽 연결되지 않은 오래된 동아리 사진을 정리한다. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ClubImageSchedule {

    private final ClubImageCleanupService cleanupService;

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredImages() {
        cleanupService.deleteExpired();
    }
}
