package com.donggree.user.internal.application;

import com.donggree.transcript.TranscriptCreatedEvent;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TranscriptEventHandler {

    private final MemberRepository memberRepository;

    @ApplicationModuleListener
    void on(TranscriptCreatedEvent event) {
        if (event.studentId() == null || event.name() == null) {
            return;
        }
        memberRepository.findById(event.memberId()).ifPresent(member -> {
            if (event.studentId().equals(member.getStudentId())
                    && event.name().equals(member.getName())) {
                member.verifyIdentity();
            }
        });
    }
}
