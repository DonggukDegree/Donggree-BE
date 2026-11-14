package com.donggree.place.internal.application;

import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.domain.Place;
import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 장소 상세 읽기 모델을 만든다. 등록자 닉네임은 복사 저장하지 않고 조회 때마다 user 모듈에서 가져오고,
 * 별점은 장소 행에 쌓지 않고 조회 때 집계한다(리뷰가 장소 버전을 올려 정보 수정 충돌을 일으키지 않게).
 * 상세 조회와 정보 수정 응답이 같은 모양이어야 FE 캐시를 그대로 바꿀 수 있어 한곳에서 만든다.
 */
@Component
@RequiredArgsConstructor
class PlaceDetailReader {

    private final UserLookupService userLookupService;
    private final PlaceQueries placeQueries;

    /** 등록자가 탈퇴했거나(등록자 ID 해제) 조회되지 않으면 닉네임은 null이다. */
    PlaceDetailReadModel read(Place place) {
        Long registrantId = place.getCreatedBy();
        String registrantNickname = registrantId == null
                ? null
                : userLookupService.findProfiles(List.of(registrantId)).values().stream()
                        .findFirst()
                        .map(MemberProfile::nickname)
                        .orElse(null);
        return PlaceDetailReadModel.from(place, registrantNickname, placeQueries.findRatingSummary(place.getId()));
    }
}
