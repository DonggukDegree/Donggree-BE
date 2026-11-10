package com.donggree.place.internal.application;

import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import java.util.List;

/** 장소 읽기 모델을 반환하는 응용 계층 조회 계약. */
public interface PlaceQueries {

    /**
     * 지도에 표시할 숨기지 않은 장소 전체를 조회한다.
     * 서비스 영역이 캠퍼스 반경 1.5km라 화면 영역별 조회 없이 한 번에 내려준다.
     */
    List<PlaceSummaryReadModel> findVisibleSummaries();
}
