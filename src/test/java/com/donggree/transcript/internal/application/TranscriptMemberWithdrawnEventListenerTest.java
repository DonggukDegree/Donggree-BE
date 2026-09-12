package com.donggree.transcript.internal.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptMemberWithdrawnEventListenerTest {

    @InjectMocks
    private TranscriptMemberWithdrawnEventListener listener;

    @Mock
    private TranscriptRepository transcriptRepository;

    @Test
    void 회원_탈퇴_이벤트를_수신하면_해당_회원의_성적표를_물리_삭제한다() {
        Long memberId = 1L;
        Transcript transcript = createTranscript(memberId);
        given(transcriptRepository.findByMemberId(memberId)).willReturn(Optional.of(transcript));

        listener.on(new MemberWithdrawnEvent(memberId));

        verify(transcriptRepository).delete(transcript);
    }

    @Test
    void 탈퇴_회원의_성적표가_없으면_삭제하지_않는다() {
        Long memberId = 999L;
        given(transcriptRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        listener.on(new MemberWithdrawnEvent(memberId));

        verify(transcriptRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private Transcript createTranscript(Long memberId) {
        return Transcript.create(new TranscriptCreateData(
                memberId,
                "{\"pages\": []}",
                2023,
                "재학",
                "단일",
                100L,
                null,
                null,
                null,
                null,
                80,
                new BigDecimal("3.95"),
                4,
                "S1",
                true,
                false,
                false,
                false,
                true,
                true,
                null,
                null,
                false));
    }
}
