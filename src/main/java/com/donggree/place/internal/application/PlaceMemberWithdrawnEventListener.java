package com.donggree.place.internal.application;

import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴 시 장소는 남기고 등록자 연결만 끊는다.
 *
 * <p>탈퇴 회원이 같은 카카오 계정으로 다시 로그인하면 같은 회원 ID가 재활성화되므로,
 * ID를 남기면 재가입자가 이전 기여의 작성자로 다시 연결된다. 재처리되어도 결과가 같다(멱등).
 */
@Component
@RequiredArgsConstructor
public class PlaceMemberWithdrawnEventListener {

    private final PlaceRepository placeRepository;

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        placeRepository.clearCreatedBy(event.memberId());
    }
}
