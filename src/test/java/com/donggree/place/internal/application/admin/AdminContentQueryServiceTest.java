package com.donggree.place.internal.application.admin;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class AdminContentQueryServiceTest {

    private static final AdminMemberSummary AUTHOR =
            new AdminMemberSummary(7L, "김아코", "김아코", "2026000001", "AI융합대학", "컴퓨터공학전공", null);

    private final PlaceQueries placeQueries = mock(PlaceQueries.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final AdminMemberReader memberReader = mock(AdminMemberReader.class);
    private final AdminContentQueryService service =
            new AdminContentQueryService(placeQueries, placeRepository, memberReader);

    @Test
    void 장소_목록은_검색어를_다듬고_대기_신고_수와_등록자를_붙이며_탈퇴_등록자는_null이다() {
        Place withRegistrant = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        ReflectionTestUtils.setField(withRegistrant, "createdBy", 7L);
        Place withdrawn = PlaceFixtures.place(11L, "필동반점", NEAR_CAMPUS);
        ReflectionTestUtils.setField(withdrawn, "createdBy", null);
        given(placeQueries.findPlacesForAdmin(eq(PlaceStatus.VISIBLE), eq("반점"), any()))
                .willReturn(new PageImpl<>(List.of(withRegistrant, withdrawn), PageRequest.of(0, 20), 2));
        given(placeQueries.countPendingPlaceReports(List.of(10L, 11L))).willReturn(Map.of(10L, 2L));
        given(memberReader.read(anyCollection())).willReturn(Map.of(7L, AUTHOR));

        var page = service.getPlaces(PlaceStatus.VISIBLE, "  반점 ", 0);

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items().get(0).pendingReportCount()).isEqualTo(2);
        assertThat(page.items().get(0).registrant()).isEqualTo(AUTHOR);
        assertThat(page.items().get(1).pendingReportCount()).isZero();
        assertThat(page.items().get(1).registrant()).isNull();
    }

    @Test
    void 리뷰_목록은_숨기거나_지운_리뷰도_본문과_장소_이름을_담는다() {
        PlaceReview hidden = PlaceReview.write(10L, 7L, 1, "광고 문구가 들어간 리뷰");
        ReflectionTestUtils.setField(hidden, "id", 30L);
        hidden.hide();
        PlaceReview deleted = PlaceReview.write(10L, 8L, 3, "작성자가 지운 리뷰입니다");
        ReflectionTestUtils.setField(deleted, "id", 29L);
        ReflectionTestUtils.setField(deleted, "authorId", null);
        deleted.delete(LocalDateTime.now());
        given(placeQueries.findReviewsForAdmin(isNull(), isNull(), any()))
                .willReturn(new PageImpl<>(List.of(hidden, deleted), PageRequest.of(0, 20), 2));
        given(placeQueries.countPendingReviewReports(List.of(30L, 29L))).willReturn(Map.of(30L, 3L));
        given(placeRepository.findAllById(List.of(10L)))
                .willReturn(List.of(PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS)));
        given(memberReader.read(anyCollection())).willReturn(Map.of(7L, AUTHOR));

        var page = service.getReviews(null, " ", 0);

        var first = page.items().get(0);
        assertThat(first.content()).isEqualTo("광고 문구가 들어간 리뷰");
        assertThat(first.placeName()).isEqualTo("동국반점");
        assertThat(first.pendingReportCount()).isEqualTo(3);
        assertThat(first.author()).isEqualTo(AUTHOR);
        assertThat(page.items().get(1).deleted()).isTrue();
        assertThat(page.items().get(1).author()).isNull();
    }

    @Test
    void 리뷰_상태_필터를_그대로_넘긴다() {
        given(placeQueries.findReviewsForAdmin(eq(AdminReviewState.DELETED), isNull(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(service.getReviews(AdminReviewState.DELETED, null, 0).items())
                .isEmpty();
    }
}
