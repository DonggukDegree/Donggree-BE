package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubDetailReadModel;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubImageRepository;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClubQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2027, 3, 2, 12, 0);

    private final ClubQueries queries = mock(ClubQueries.class);
    private final ClubRepository clubs = mock(ClubRepository.class);
    private final ClubRecruitmentRepository recruitments = mock(ClubRecruitmentRepository.class);
    private final ClubEditorRepository editors = mock(ClubEditorRepository.class);
    private final ClubImageRepository images = mock(ClubImageRepository.class);
    private final ClubQueryService service = new ClubQueryService(
            queries, clubs, recruitments, editors, images, () -> NOW, new ClubImageUrls(ClubImageUrls.API_PATH));

    @Test
    void 없는_동아리는_404다() {
        given(clubs.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClub(7L, 9L))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND));
    }

    @Test
    void 기수_활동이_끝난_뒤_마감된_모집은_지난_모집으로_준다() {
        Club club = Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, NOW.minusYears(1));
        ClubRecruitment closed =
                ClubRecruitment.open(1L, ClubFixtures.period(NOW.minusMonths(12), NOW.minusMonths(11)));
        given(clubs.findById(1L)).willReturn(Optional.of(club));
        given(recruitments.findTopByClubIdOrderByIdDesc(1L)).willReturn(Optional.of(closed));

        given(editors.canEdit(1L, 7L)).willReturn(true);

        ClubDetailReadModel detail = service.getClub(7L, 1L);

        assertThat(detail.recruitment()).isNull();
        assertThat(detail.previousRecruitment().generation()).isEqualTo(25);
        assertThat(detail.activityConfirmed()).isFalse();
        assertThat(detail.canEditDirectly()).isTrue();
        assertThat(detail.images()).isEmpty();
    }

    @Test
    void 형식이_맞지_않는_커서는_400이다() {
        given(queries.findListRows()).willReturn(List.of());

        assertThatThrownBy(() -> service.getClubs(new ClubSearch(null, null, null, null, null), "%%%"))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.INVALID_CURSOR));
    }
}
