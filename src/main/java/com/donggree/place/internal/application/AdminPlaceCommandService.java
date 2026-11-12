package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistory;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 동글맵 장소 관리. 신고 확인 후 숨기거나(신고 처리 완료) 숨김을 해제한다(대기 신고 기각).
 * 이름·위치·카테고리 정정과 삭제·병합, 신고 기각은 MVP에서 DB로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminPlaceCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceReportRepository reportRepository;
    private final PlaceEditHistoryRepository editHistoryRepository;

    /** 장소를 숨기고 그 장소의 처리 대기 신고를 처리 완료로 바꾼다. 이미 숨긴 장소도 같은 결과다. */
    @Transactional
    public void hide(Long placeId) {
        Place place = find(placeId);
        place.hide();
        reportRepository.resolvePending(placeId, LocalDateTime.now());
    }

    /**
     * 숨김을 해제하고 그 장소의 처리 대기 신고를 기각한다.
     * 자동 숨김을 관리자가 풀었다는 것은 신고를 받아들이지 않았다는 뜻이고, 대기로 두면 다음 신고 한 건에 다시 숨겨진다.
     */
    @Transactional
    public void show(Long placeId) {
        find(placeId).show();
        reportRepository.rejectPending(placeId, LocalDateTime.now());
    }

    /**
     * 수정 이력 한 건의 수정 전 값으로 장소 정보를 되돌린다(부적절한 수정 복구). 되돌린 것도 관리자 수정으로 이력에 남는다.
     * 버전 충돌 확인 없이 관리자가 바로 덮어쓰며, 지금 값과 같으면 아무것도 바꾸지 않는다.
     */
    @Transactional
    public void revertEdit(Long placeId, Long historyId, Long adminId) {
        Place place = find(placeId);
        PlaceEditHistory history = editHistoryRepository
                .findByIdAndPlaceId(historyId, placeId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.EDIT_HISTORY_NOT_FOUND));
        place.editInfo(adminId, history.before()).ifPresent(editHistoryRepository::save);
    }

    private Place find(Long placeId) {
        return placeRepository
                .findById(placeId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
    }
}
