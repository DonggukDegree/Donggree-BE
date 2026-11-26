package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubClock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class KoreaClubClock implements ClubClock {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
