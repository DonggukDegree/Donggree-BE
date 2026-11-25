package com.donggree.mystery.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.mystery.event.MysterySolvedEvent;
import com.donggree.mystery.internal.application.command.MysteryGuessCommand;
import com.donggree.mystery.internal.application.exception.MysteryErrorCode;
import com.donggree.mystery.internal.application.readmodel.MysteryTodayReadModel;
import com.donggree.mystery.internal.domain.*;
import com.donggree.mystery.internal.domain.MysteryData.*;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 진행 갱신·판정·단서 저장은 회원별 잠금 안에서 한 번에 처리한다. */
@Service
@RequiredArgsConstructor
public class MysteryCommandService {
    private final MysteryCaseService cases;
    private final MysteryPlayerRepository players;
    private final MysteryCalendar clock;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public MysteryTodayReadModel submit(Long memberId, MysteryGuessCommand command) {
        players.createIfAbsent(memberId);
        var player = players.lockByMemberId(memberId).orElseThrow();
        var date = clock.today();
        if (!date.equals(command.gameDate())) throw new GeneralException(MysteryErrorCode.DATE_CHANGED);
        player.openDay(date);
        var daily = cases.getOrCreate(date);
        var data = daily.getData();
        var previous = player.getSession().attempts().stream()
                .filter(a -> a.requestId().equals(command.requestId()))
                .findFirst();
        if (previous.isPresent()) {
            if (!previous.get().guess().equals(command.guess()))
                throw new GeneralException(MysteryErrorCode.REQUEST_REUSED);
            return MysteryView.of(daily, player);
        }
        if (player.finished()) throw new GeneralException(MysteryErrorCode.FINISHED);
        if (!data.contains(command.guess())) throw new GeneralException(MysteryErrorCode.INVALID_OPTION);
        if (player.getSession().attempts().stream().anyMatch(a -> a.guess().equals(command.guess())))
            throw new GeneralException(MysteryErrorCode.DUPLICATE);
        player.start(data);
        int number = player.getSession().attempts().size() + 1;
        boolean correct = data.answer().equals(command.guess());
        Clue clue = correct || number == 10
                ? null
                : MysteryClues.generate(data, player.getSession(), command.guess(), number, random);
        player.record(new Attempt(command.requestId(), command.guess(), correct, clue));
        if (correct) events.publishEvent(new MysterySolvedEvent(memberId, date));
        return MysteryView.of(daily, player);
    }
}
