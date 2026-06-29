package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
    void 회원_탈퇴_이벤트를_수신하면_해당_회원의_성적표를_소프트_삭제한다() {
        Long memberId = 1L;
        Transcript transcript = createTranscript(memberId);
        given(transcriptRepository.findByMemberId(memberId)).willReturn(Optional.of(transcript));

        listener.on(new MemberWithdrawnEvent(memberId));

        assertThat(transcript.isDeleted()).isTrue();
    }

    @Test
    void 탈퇴_회원의_성적표가_없으면_아무_동작도_하지_않는다() {
        Long memberId = 999L;
        given(transcriptRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        listener.on(new MemberWithdrawnEvent(memberId));
        // 예외 없이 정상 종료되면 통과 (조회 결과가 없으면 삭제 시도하지 않음)
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
                false));
    }
}
