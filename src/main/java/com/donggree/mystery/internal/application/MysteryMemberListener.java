package com.donggree.mystery.internal.application;

import com.donggree.mystery.internal.domain.MysteryPlayerRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MysteryMemberListener {
    private final MysteryPlayerRepository players;

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        players.deleteById(event.memberId());
    }
}
