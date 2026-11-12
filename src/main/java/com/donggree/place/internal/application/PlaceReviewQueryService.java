package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceReviewPageReadModel;
import com.donggree.place.internal.application.readmodel.PlaceReviewReadModel;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 리뷰 조회 응용 서비스. 로그인 회원 누구나 본다.
 * 작성자 닉네임·프로필은 복사 저장하지 않고 페이지마다 user 모듈에서 한 번에 가져온다.
 * 숨긴 리뷰는 목록에서 자리를 지키되 본문·별점을 비워 보낸다(화면은 "신고되어 숨겨진 리뷰입니다."로 표시).
 */
@Service
@RequiredArgsConstructor
public class PlaceReviewQueryService {

    public static final int PAGE_SIZE = 10;

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository reviewRepository;
    private final PlaceQueries placeQueries;
    private final UserLookupService userLookupService;

    /**
     * 작성자가 지우지 않은 리뷰를 최신순으로 10개씩 돌려준다. 숨긴 리뷰는 본문·별점 없이 넣는다.
     * 요청 회원의 리뷰는 숨겨졌어도 본문까지 myReview로 따로 준다.
     *
     * @param cursor 이전 페이지의 nextCursor. 첫 페이지는 null
     */
    @Transactional(readOnly = true)
    public PlaceReviewPageReadModel getReviews(Long memberId, Long placeId, Long cursor) {
        if (placeRepository.findByIdAndStatus(placeId, PlaceStatus.VISIBLE).isEmpty()) {
            throw new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        List<PlaceReview> fetched = placeQueries.findListedReviews(placeId, cursor, PAGE_SIZE + 1);
        boolean hasNext = fetched.size() > PAGE_SIZE;
        List<PlaceReview> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
        Optional<PlaceReview> mine = reviewRepository.findByPlaceIdAndAuthorIdAndDeletedAtIsNull(placeId, memberId);

        List<Long> authorIds =
                new ArrayList<>(page.stream().map(PlaceReview::getAuthorId).toList());
        mine.ifPresent(review -> authorIds.add(review.getAuthorId()));
        Map<Long, MemberProfile> profiles = userLookupService.findProfiles(authorIds);

        return new PlaceReviewPageReadModel(
                page.stream()
                        .map(review -> toReadModel(review, memberId, profiles, !review.isVisible()))
                        .toList(),
                hasNext ? page.getLast().getId() : null,
                mine.map(review -> toReadModel(review, memberId, profiles, false))
                        .orElse(null));
    }

    /** @param masked 본문·별점을 비울지. 숨긴 리뷰를 목록에 넣을 때만 true */
    private static PlaceReviewReadModel toReadModel(
            PlaceReview review, Long memberId, Map<Long, MemberProfile> profiles, boolean masked) {
        MemberProfile author = review.getAuthorId() == null ? null : profiles.get(review.getAuthorId());
        return new PlaceReviewReadModel(
                review.getId(),
                masked ? null : review.getRating(),
                masked ? null : review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                author == null ? null : author.nickname(),
                author == null ? null : author.profileUrl(),
                review.isAuthor(memberId),
                !review.isVisible());
    }
}
