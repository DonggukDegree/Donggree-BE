package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.EditPlaceInfoCommand;
import com.donggree.place.internal.application.command.RegisterPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.domain.CampusArea;
import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistory;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceInfo;
import com.donggree.place.internal.domain.PlaceRegistration;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동글맵 장소 변경(Command) 응용 서비스.
 * 쓰기 권한(PDF 본인인증 또는 학교 이메일 인증)은 FE 표시와 별개로 매 요청 다시 검증한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceEditHistoryRepository editHistoryRepository;
    private final PlaceDuplicateFinder duplicateFinder;
    private final UserLookupService userLookupService;
    private final PlaceDetailReader detailReader;

    /**
     * 장소를 등록하고 ID를 반환한다. 반경 50m 안에 정규화 이름이 같은 장소가 있으면 막는다.
     * 이름이 비슷한 후보는 FE가 등록 전 안내하고 사용자가 확인했으므로 여기서는 막지 않는다.
     */
    @Transactional
    public Long register(Long memberId, RegisterPlaceCommand command) {
        requireStudentVerified(memberId);
        GeoPoint point = point(command.latitude(), command.longitude());
        if (!CampusArea.contains(point)) {
            throw new GeneralException(PlaceErrorCode.OUT_OF_SERVICE_AREA);
        }
        if (command.category() == null
                || command.subCategory() == null
                || !command.subCategory().belongsTo(command.category())) {
            throw new GeneralException(PlaceErrorCode.CATEGORY_MISMATCH);
        }
        if (duplicateFinder.find(command.name(), point).hasExact()) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_PLACE);
        }
        Place place;
        try {
            place = Place.register(new PlaceRegistration(
                    memberId,
                    command.name(),
                    command.category(),
                    command.subCategory(),
                    point,
                    command.businessHours(),
                    command.description(),
                    command.menus()));
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_PLACE);
        }
        return placeRepository.save(place).getId();
    }

    /**
     * 영업시간·설명·메뉴를 고치고 수정 이력을 남긴 뒤 최신 장소 정보를 반환한다.
     *
     * <p>요청 버전이 현재 버전과 다르거나, 같은 버전을 읽은 다른 수정이 먼저 커밋되면
     * 덮어쓰지 않고 409로 막는다. 바뀐 값이 없으면 이력·버전을 그대로 둔다.
     */
    @Transactional
    public PlaceDetailReadModel editInfo(Long memberId, Long placeId, EditPlaceInfoCommand command) {
        requireStudentVerified(memberId);
        Place place = placeRepository
                .findByIdAndStatus(placeId, PlaceStatus.VISIBLE)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
        if (!Objects.equals(place.getVersion(), command.version())) {
            throw new GeneralException(PlaceErrorCode.EDIT_CONFLICT);
        }
        Optional<PlaceEditHistory> history;
        try {
            history = place.editInfo(
                    memberId, new PlaceInfo(command.businessHours(), command.description(), command.menus()));
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_PLACE);
        }
        if (history.isEmpty()) {
            return detailReader.read(place);
        }
        try {
            // 버전 조건 UPDATE를 여기서 실행해 동시 수정 충돌을 응답 코드로 바꾼다.
            placeRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new GeneralException(PlaceErrorCode.EDIT_CONFLICT);
        }
        editHistoryRepository.save(history.get());
        return detailReader.read(place);
    }

    private void requireStudentVerified(Long memberId) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
    }

    private static GeoPoint point(double latitude, double longitude) {
        try {
            return new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_PLACE);
        }
    }
}
