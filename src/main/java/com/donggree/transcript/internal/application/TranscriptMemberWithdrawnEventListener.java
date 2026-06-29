package com.donggree.transcript.internal.application;

import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * user 모듈의 회원 탈퇴 이벤트를 수신하여 해당 회원의 성적표를 정리하는 이벤트 핸들러.
 *
 * <p>탈퇴는 데이터 복원이 아닌 파기를 의도하므로(재가입 시 새 회원처럼 시작),
 * 회원의 Transcript를 물리 삭제한다. 성적표(raw_data·학점·평점 등)는 민감한 개인정보이므로
 * 개인정보 보호 관점에서 복구 불가능하게 영구 파기하며, 소프트 삭제로 인한 고립 데이터 누적과
 * 재가입 시 member_id unique 제약 충돌도 함께 방지한다.
 * Transcript에 {@code cascade = CascadeType.ALL, orphanRemoval = true}가 설정되어 있어
 * 연관된 CourseRecord도 함께 삭제된다.
 *
 * <p>{@code findByMemberId}는 이미 삭제된 성적표를 반환하지 않으므로,
 * 이벤트가 재처리되어도 중복 삭제가 일어나지 않는다(멱등).
 */
@Component
@RequiredArgsConstructor
public class TranscriptMemberWithdrawnEventListener {

    private final TranscriptRepository transcriptRepository;

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        transcriptRepository.findByMemberId(event.memberId()).ifPresent(transcriptRepository::delete);
    }
}
