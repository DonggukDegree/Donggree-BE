package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.application.readmodel.PlaceDuplicatesReadModel;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 동글맵 장소 조회(Query) 응용 서비스. 로그인 회원 누구나 조회한다. */
@Service
@RequiredArgsConstructor
public class PlaceQueryService {

    private final PlaceQueries placeQueries;
    private final PlaceRepository placeRepository;
    private final PlaceDuplicateFinder duplicateFinder;

    @Transactional(readOnly = true)
    public List<PlaceSummaryReadModel> getPlaces() {
        return placeQueries.findVisibleSummaries();
    }

    /** 숨김 장소는 존재하지 않는 장소와 같게 응답한다. */
    @Transactional(readOnly = true)
    public PlaceDetailReadModel getPlace(Long placeId) {
        return placeRepository
                .findByIdAndStatus(placeId, PlaceStatus.VISIBLE)
                .map(PlaceDetailReadModel::from)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PlaceDuplicatesReadModel findDuplicates(String name, double latitude, double longitude) {
        return PlaceDuplicatesReadModel.from(duplicateFinder.find(name, point(latitude, longitude)));
    }

    private static GeoPoint point(double latitude, double longitude) {
        try {
            return new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_PLACE);
        }
    }
}
