package com.donggree.place.internal.application;

import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.PlaceDuplicates;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 반경 후보를 DB에서 사각 범위로 좁힌 뒤 도메인 규칙으로 중복을 판정한다. 등록 전 안내와 등록 시 차단이 같이 쓴다. */
@Component
@RequiredArgsConstructor
class PlaceDuplicateFinder {

    private final PlaceRepository placeRepository;

    PlaceDuplicates find(String name, GeoPoint point) {
        GeoPoint.BoundingBox box = point.boundingBox(PlaceDuplicates.RADIUS_METERS);
        return PlaceDuplicates.of(
                name,
                point,
                placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        PlaceStatus.VISIBLE,
                        box.minLatitude(),
                        box.maxLatitude(),
                        box.minLongitude(),
                        box.maxLongitude()));
    }
}
