package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.EditPlaceInfoCommand;
import com.donggree.place.internal.application.command.RegisterPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistory;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.user.UserLookupService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceCommandServiceTest {

    private static final Long MEMBER_ID = 7L;

    private static final Long PLACE_ID = 10L;

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceEditHistoryRepository editHistoryRepository = mock(PlaceEditHistoryRepository.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final PlaceCommandService service = new PlaceCommandService(
            placeRepository,
            editHistoryRepository,
            new PlaceDuplicateFinder(placeRepository),
            userLookupService,
            new PlaceDetailReader(userLookupService));

    @BeforeEach
    void setUp() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(true);
        given(placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        eq(PlaceStatus.VISIBLE), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(placeRepository.save(any(Place.class))).willAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            ReflectionTestUtils.setField(place, "id", 100L);
            return place;
        });
    }

    @Test
    void 인증_회원은_장소를_등록하고_ID를_받는다() {
        Long placeId = service.register(MEMBER_ID, command("동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE));

        assertThat(placeId).isEqualTo(100L);
        ArgumentCaptor<Place> saved = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(MEMBER_ID);
        assertThat(saved.getValue().getMenus()).hasSize(1);
    }

    @Test
    void 미인증_회원은_403으로_막는다() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(false);

        assertError(
                () -> service.register(MEMBER_ID, command("동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE)),
                PlaceErrorCode.VERIFICATION_REQUIRED);
    }

    @Test
    void 서비스_영역_밖이면_400_1로_막는다() {
        RegisterPlaceCommand outside = new RegisterPlaceCommand(
                "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, 37.6, 127.0, null, null, List.of());

        assertError(() -> service.register(MEMBER_ID, outside), PlaceErrorCode.OUT_OF_SERVICE_AREA);
    }

    @Test
    void 카테고리_짝이_맞지_않으면_400_2로_막는다() {
        assertError(
                () -> service.register(MEMBER_ID, command("동국반점", PlaceCategory.CAFE, PlaceSubCategory.CHINESE)),
                PlaceErrorCode.CATEGORY_MISMATCH);
    }

    @Test
    void 반경_50m_안에_같은_이름이_있으면_409로_막는다() {
        given(placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        eq(PlaceStatus.VISIBLE), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(PlaceFixtures.place(1L, "동국 반점", NEAR_CAMPUS)));

        assertError(
                () -> service.register(MEMBER_ID, command("동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE)),
                PlaceErrorCode.DUPLICATE_PLACE);
    }

    @Test
    void 이름이_비슷한_후보만_있으면_등록한다() {
        given(placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        eq(PlaceStatus.VISIBLE), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(PlaceFixtures.place(1L, "동국반점 2호점", NEAR_CAMPUS)));

        assertThat(service.register(MEMBER_ID, command("동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE)))
                .isEqualTo(100L);
    }

    @Test
    void 도메인_검증에_실패하면_400_3으로_응답한다() {
        assertError(
                () -> service.register(
                        MEMBER_ID, command("가".repeat(31), PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE)),
                PlaceErrorCode.INVALID_PLACE);
    }

    @Test
    void 인증_회원은_정보를_수정하고_이력을_남긴_뒤_최신_정보를_받는다() {
        Place place = storedPlace(3L);

        PlaceDetailReadModel result = service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "매일 10:00-22:00"));

        assertThat(result.businessHours()).isEqualTo("매일 10:00-22:00");
        assertThat(result.menus()).extracting(PlaceDetailReadModel.Menu::name).containsExactly("짬뽕");
        verify(placeRepository).flush();
        ArgumentCaptor<PlaceEditHistory> saved = ArgumentCaptor.forClass(PlaceEditHistory.class);
        verify(editHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getEditorId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getValue().getPlaceId()).isEqualTo(PLACE_ID);
        assertThat(saved.getValue().getBeforeBusinessHours()).isEqualTo("평일 11:00-21:00");
        assertThat(place.getCreatedBy()).isEqualTo(1L);
    }

    @Test
    void 바뀐_값이_없으면_이력을_남기지_않는다() {
        Place place = storedPlace(3L);

        service.editInfo(
                MEMBER_ID,
                PLACE_ID,
                new EditPlaceInfoCommand(
                        3L,
                        place.getBusinessHours(),
                        place.getDescription(),
                        place.info().menus()));

        verify(placeRepository, never()).flush();
        verify(editHistoryRepository, never()).save(any());
    }

    @Test
    void 화면을_연_뒤_다른_수정이_있었으면_409_2로_막는다() {
        Place place = storedPlace(4L);

        assertEditError(
                () -> service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "매일 10:00-22:00")), PlaceErrorCode.EDIT_CONFLICT);
        assertThat(place.getBusinessHours()).isEqualTo("평일 11:00-21:00");
    }

    @Test
    void 같은_버전으로_동시에_저장하면_나중_저장을_409_2로_막는다() {
        storedPlace(3L);
        willThrow(new ObjectOptimisticLockingFailureException(Place.class, PLACE_ID))
                .given(placeRepository)
                .flush();

        assertEditError(
                () -> service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "매일 10:00-22:00")), PlaceErrorCode.EDIT_CONFLICT);
    }

    @Test
    void 미인증_회원의_수정은_403으로_막는다() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(false);

        assertEditError(
                () -> service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "매일 10:00-22:00")),
                PlaceErrorCode.VERIFICATION_REQUIRED);
        verify(placeRepository, never()).findByIdAndStatus(any(), any());
    }

    @Test
    void 없거나_숨긴_장소의_수정은_404로_막는다() {
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.empty());

        assertEditError(
                () -> service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "매일 10:00-22:00")),
                PlaceErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void 수정값_검증에_실패하면_400_3으로_응답한다() {
        storedPlace(3L);

        assertEditError(
                () -> service.editInfo(MEMBER_ID, PLACE_ID, edit(3L, "가".repeat(101))), PlaceErrorCode.INVALID_PLACE);
    }

    private Place storedPlace(Long version) {
        Place place = PlaceFixtures.place(PLACE_ID, "동국반점", NEAR_CAMPUS);
        ReflectionTestUtils.setField(place, "version", version);
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.of(place));
        return place;
    }

    private static EditPlaceInfoCommand edit(Long version, String businessHours) {
        return new EditPlaceInfoCommand(version, businessHours, null, List.of(new MenuItem("짬뽕", 8000)));
    }

    private void assertEditError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
        verify(editHistoryRepository, never()).save(any());
    }

    private static RegisterPlaceCommand command(String name, PlaceCategory category, PlaceSubCategory subCategory) {
        return new RegisterPlaceCommand(
                name,
                category,
                subCategory,
                NEAR_CAMPUS.latitude(),
                NEAR_CAMPUS.longitude(),
                null,
                null,
                List.of(new MenuItem("짜장면", 7000)));
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
        verify(placeRepository, never()).save(any(Place.class));
    }
}
