package com.donggree.place.internal.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.Test;

class PlaceMemberWithdrawnEventListenerTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceEditHistoryRepository editHistoryRepository = mock(PlaceEditHistoryRepository.class);
    private final PlaceMemberWithdrawnEventListener listener =
            new PlaceMemberWithdrawnEventListener(placeRepository, editHistoryRepository);

    @Test
    void 탈퇴_회원의_장소_등록자와_수정_이력_수정자_연결을_끊는다() {
        listener.on(new MemberWithdrawnEvent(7L));

        verify(placeRepository).clearCreatedBy(7L);
        verify(editHistoryRepository).clearEditor(7L);
    }
}
