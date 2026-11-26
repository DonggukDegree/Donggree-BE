package com.donggree.club.internal.application;

import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴 시 요청 내용·처리 이력은 남기고 요청자 연결·연락처·동아리와의 관계를 비우며, 직접 수정 권한을 없앤다.
 * 같은 회원 ID로 재가입해도 이전 요청·권한에 다시 연결되지 않는다. 재처리되어도 결과가 같다(멱등).
 */
@Component
@RequiredArgsConstructor
public class ClubMemberWithdrawnEventListener {

    private final ClubRequestRepository requestRepository;
    private final ClubEditorRepository editorRepository;

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        requestRepository.clearRequester(event.memberId());
        editorRepository.deleteByMember(event.memberId());
    }
}
