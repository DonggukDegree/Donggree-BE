package com.donggree.club.internal.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.club.internal.application.command.BannerSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.BannerContent;
import com.donggree.club.internal.domain.ClubBanner;
import com.donggree.club.internal.domain.ClubBannerRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminClubBannerCommandServiceTest {

    private final ClubBannerRepository banners = mock(ClubBannerRepository.class);
    private final AdminClubBannerCommandService service = new AdminClubBannerCommandService(banners);

    @Test
    void 새_배너는_맨_뒤_순서로_등록한다() {
        given(banners.findMaxSortOrder()).willReturn(2);
        given(banners.save(any(ClubBanner.class))).willAnswer(invocation -> {
            ClubBanner saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 9L);
            return saved;
        });

        assertThat(service.create(command("#2563eb"))).isEqualTo(9L);
    }

    @Test
    void 잘못된_내용은_400이다() {
        assertThatThrownBy(() -> service.create(command(null)))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.INVALID_BANNER));
    }

    @Test
    void 한_칸씩_순서를_바꾸고_끝에서는_그대로_둔다() {
        ClubBanner first = banner(1L, 1);
        ClubBanner second = banner(2L, 2);
        given(banners.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(first, second));

        service.move(2L, -1);

        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);

        given(banners.findAllByOrderBySortOrderAscIdAsc()).willReturn(List.of(second, first));
        service.move(2L, -1);
        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThatThrownBy(() -> service.move(9L, 1)).isInstanceOf(GeneralException.class);
    }

    private static ClubBanner banner(Long id, int order) {
        ClubBanner banner = ClubBanner.create(
                new BannerContent("배너", null, null, "#2563eb", null, null, "/clubs", null, null, true), order);
        ReflectionTestUtils.setField(banner, "id", id);
        return banner;
    }

    private static BannerSaveCommand command(String background) {
        return new BannerSaveCommand(
                "우리 동아리가 없나요?", "EVENT", null, background, null, null, "/clubs/register", null, null, true);
    }
}
