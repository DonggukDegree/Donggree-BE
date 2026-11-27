package com.donggree.club.internal.application;

import com.donggree.club.internal.application.readmodel.ClubBannerReadModel;
import com.donggree.club.internal.domain.ClubBannerRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배너 조회. 공개 목록은 켜져 있고 노출 기간 안인 배너만, 관리자 목록은 전체를 노출 순서대로 준다. */
@Service
@RequiredArgsConstructor
public class ClubBannerQueryService {

    private final ClubBannerRepository bannerRepository;
    private final ClubClock clock;

    @Transactional(readOnly = true)
    public List<ClubBannerReadModel> getVisibleBanners() {
        LocalDateTime now = clock.now();
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(banner -> banner.content().visibleAt(now))
                .map(ClubBannerReadModel::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClubBannerReadModel> getAllBanners() {
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ClubBannerReadModel::from)
                .toList();
    }
}
