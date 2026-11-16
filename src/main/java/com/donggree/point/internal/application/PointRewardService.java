package com.donggree.point.internal.application;

import com.donggree.point.internal.domain.PointHistory;
import com.donggree.point.internal.domain.PointHistoryRepository;
import com.donggree.point.internal.domain.PointReason;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활동별 포인트 적립·회수. 모든 적립은 중복 무시 INSERT라 같은 이벤트를 여러 번 처리해도 결과가 같다(멱등).
 *
 * <ul>
 *   <li>졸업 리포트 최초 확인 500P: 회원당 1회
 *   <li>장소 등록 500P: 장소당
 *   <li>리뷰: 그 장소의 첫 리뷰면 500P, 아니면 100P. 지웠다 다시 써도 회원당 장소별 1회
 *   <li>장소 정보 수정 100P: 회원당 장소별 1회
 *   <li>장소·리뷰가 숨겨지면 해당 적립을 회수하고, 숨김이 풀리면 되살린다. 본인 삭제는 회수하지 않는다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PointRewardService {

    static final List<PointReason> REVIEW_REASONS = List.of(PointReason.PLACE_FIRST_REVIEW, PointReason.PLACE_REVIEW);
    private static final List<PointReason> REGISTER_REASONS = List.of(PointReason.PLACE_REGISTER);

    private final PointHistoryRepository historyRepository;

    /** 조회마다 불리므로 이미 받은 회원은 쓰기 없이 끝낸다. */
    @Transactional
    public void rewardReportFirstView(Long memberId) {
        if (historyRepository.existsByMemberIdAndReason(memberId, PointReason.GRADUATION_REPORT_FIRST_VIEW)) {
            return;
        }
        grant(memberId, PointReason.GRADUATION_REPORT_FIRST_VIEW, null);
    }

    @Transactional
    public void rewardPlaceRegister(Long memberId, Long placeId) {
        grant(memberId, PointReason.PLACE_REGISTER, placeId);
    }

    /**
     * 첫 리뷰 적립은 장소별 1건 유일 제약으로 한 명만 받는다. 이미 다른 회원이 받았거나 동시에 먼저 받았으면 일반 리뷰로 적립하고,
     * 이 회원이 이 장소에서 리뷰 적립을 이미 받았으면 둘 다 넣지 않는다.
     */
    @Transactional
    public void rewardReview(Long memberId, Long placeId, boolean firstReviewOfPlace) {
        if (firstReviewOfPlace && grant(memberId, PointReason.PLACE_FIRST_REVIEW, placeId)) {
            return;
        }
        grant(memberId, PointReason.PLACE_REVIEW, placeId);
    }

    @Transactional
    public void rewardInfoEdit(Long memberId, Long placeId) {
        grant(memberId, PointReason.PLACE_INFO_EDIT, placeId);
    }

    @Transactional
    public void onPlaceVisibilityChanged(Long placeId, boolean visible) {
        if (visible) {
            historyRepository.restore(placeId, REGISTER_REASONS);
        } else {
            historyRepository.revoke(placeId, REGISTER_REASONS, LocalDateTime.now());
        }
    }

    /** 작성자가 탈퇴해 연결이 끊긴 리뷰는 포인트 내역도 이미 지워졌으므로 할 일이 없다. */
    @Transactional
    public void onReviewVisibilityChanged(Long authorId, Long placeId, boolean visible) {
        if (authorId == null) {
            return;
        }
        if (visible) {
            historyRepository.restoreOfMember(authorId, placeId, REVIEW_REASONS);
        } else {
            historyRepository.revokeOfMember(authorId, placeId, REVIEW_REASONS, LocalDateTime.now());
        }
    }

    /**
     * 화면이 아직 안내하지 않은 적립이면 안내한 것으로 기록하고 적립액을 돌려준다.
     * 적립이 없거나, 이미 안내했거나, 회수된 적립이면 비어 있다. 같은 적립을 두 번 안내하지 않기 위함이다.
     *
     * @param targetId 장소 활동이면 장소 ID, 졸업 리포트 최초 확인이면 null
     */
    @Transactional
    public Optional<Integer> acknowledge(Long memberId, PointReason reason, Long targetId) {
        Optional<PointHistory> history = targetId == null
                ? historyRepository.findByMemberIdAndReasonAndTargetIdIsNull(memberId, reason)
                : historyRepository.findByMemberIdAndReasonAndTargetId(memberId, reason, targetId);
        return history.filter(found -> !found.isRevoked())
                .filter(found -> historyRepository.markNotified(found.getId(), LocalDateTime.now()) > 0)
                .map(PointHistory::getAmount);
    }

    @Transactional
    public void deleteAllOfMember(Long memberId) {
        historyRepository.deleteAllOfMember(memberId);
    }

    private boolean grant(Long memberId, PointReason reason, Long targetId) {
        return historyRepository.grant(memberId, reason.name(), reason.amount(), targetId, LocalDateTime.now()) > 0;
    }
}
