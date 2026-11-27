package com.donggree.club.internal.application;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.MyClubRequestDetailReadModel;
import com.donggree.club.internal.application.readmodel.MyClubRequestReadModel;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 동아리 요청 조회(Query) 응용 서비스. 다른 회원의 요청은 없는 요청과 같게 응답한다. */
@Service
@RequiredArgsConstructor
public class ClubRequestQueryService {

    private final ClubRequestRepository requestRepository;
    private final ClubPhotoBinder photoBinder;

    /** 최신순. 바로 반영된 변경은 요청이 아니라 여기에 없다. */
    @Transactional(readOnly = true)
    public List<MyClubRequestReadModel> getMyRequests(Long memberId) {
        return requestRepository.findByRequesterIdOrderByIdDesc(memberId).stream()
                .map(MyClubRequestReadModel::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyClubRequestDetailReadModel getMyRequest(Long memberId, Long requestId) {
        return requestRepository
                .findById(requestId)
                .filter(request -> request.isRequestedBy(memberId))
                .map(request -> MyClubRequestDetailReadModel.from(
                        request,
                        request.getAfter().photos() == null
                                ? Map.of()
                                : photoBinder.urls(request.getAfter().photos().imageIds())))
                .orElseThrow(() -> new GeneralException(ClubErrorCode.REQUEST_NOT_FOUND));
    }
}
