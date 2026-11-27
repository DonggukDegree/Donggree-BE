package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.club.internal.domain.ClubFavorite;
import com.donggree.club.internal.domain.ClubFavoriteRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClubFavoriteServiceTest {

    private final ClubFavoriteRepository favorites = mock(ClubFavoriteRepository.class);
    private final ClubRepository clubs = mock(ClubRepository.class);
    private final ClubFavoriteService service =
            new ClubFavoriteService(favorites, clubs, () -> LocalDateTime.of(2026, 11, 27, 12, 0));

    @Test
    void 없는_동아리는_찜할_수_없고_이미_찜했으면_다시_저장하지_않는다() {
        given(clubs.existsById(9L)).willReturn(false);
        given(clubs.existsById(1L)).willReturn(true);
        given(favorites.existsById(new ClubFavorite.Key(7L, 1L))).willReturn(true);

        assertThatThrownBy(() -> service.add(7L, 9L)).isInstanceOf(GeneralException.class);
        service.add(7L, 1L);

        verify(favorites, never()).saveAndFlush(any());
    }

    @Test
    void 찜_해제는_없어도_그대로_끝난다() {
        service.remove(7L, 1L);

        verify(favorites).deleteById(new ClubFavorite.Key(7L, 1L));
    }
}
