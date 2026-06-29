package com.donggree.transcript.internal.application;

import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * user 모듈의 회원 탈퇴 이벤트를 수신하여 해당 회원의 성적표를 정리하는 이벤트 핸들러.
 *
 * <p>탈퇴는 데이터 복원이 아닌 파기를 의도하므로(재가입 시 새 회원처럼 시작),
 * 회원의 Transcript를 소프트 삭제하여 재가입 후 이전 성적표가 조회되지 않도록 한다.
 * CourseRecord는 Transcript 루트를 통해서만 조회되며 Transcript에 걸린
 * {@code @SQLRestriction("deleted_at IS NULL")} 덕분에 함께 조회에서 제외된다.
 *
 * <p>{@code findByMemberId}는 이미 소프트 삭제된 성적표를 반환하지 않으므로,
 * 이벤트가 재처리되어도 중복 삭제가 일어나지 않는다(멱등).
 */
@Component
@RequiredArgsConstructor
public class TranscriptMemberWithdrawnEventListener {

    private final TranscriptRepository transcriptRepository;

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        transcriptRepository
                .findByMemberId(event.memberId())
                .ifPresent(transcript -> transcript.markAsDeleted(LocalDateTime.now()));
    }
}
