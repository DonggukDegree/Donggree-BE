package com.donggree.mystery.internal.application;

import com.donggree.mystery.internal.application.readmodel.MysteryTodayReadModel;
import com.donggree.mystery.internal.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MysteryQueryService {
    private final MysteryCaseService cases;
    private final MysteryPlayerRepository players;
    private final MysteryCalendar clock;

    @Transactional
    public MysteryTodayReadModel today(Long memberId) {
        players.createIfAbsent(memberId);
        var player = players.lockByMemberId(memberId).orElseThrow();
        var date = clock.today();
        player.openDay(date);
        return MysteryView.of(cases.getOrCreate(date), player);
    }
}
