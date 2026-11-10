package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.RegisterPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.CampusArea;
import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceRegistration;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.user.UserLookupService;
import lombok.RequiredArgsConstructor;
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
    private final PlaceDuplicateFinder duplicateFinder;
    private final UserLookupService userLookupService;

    /**
     * 장소를 등록하고 ID를 반환한다. 반경 50m 안에 정규화 이름이 같은 장소가 있으면 막는다.
     * 이름이 비슷한 후보는 FE가 등록 전 안내하고 사용자가 확인했으므로 여기서는 막지 않는다.
     */
    @Transactional
    public Long register(Long memberId, RegisterPlaceCommand command) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
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

    private static GeoPoint point(double latitude, double longitude) {
        try {
            return new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_PLACE);
        }
    }
}
