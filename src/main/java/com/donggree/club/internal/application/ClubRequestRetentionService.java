package com.donggree.club.internal.application;

import com.donggree.club.internal.domain.ClubRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요청자 연락처·동아리와의 관계는 요청 후 1년만 보관한다. 요청 내용·처리 이력은 남긴다. */
@Service
@RequiredArgsConstructor
public class ClubRequestRetentionService {

    static final int RETENTION_YEARS = 1;

    private final ClubRequestRepository requestRepository;
    private final ClubClock clock;

    /** @return 비운 요청 수 */
    @Transactional
    public int clearExpiredRequesterInfo() {
        return requestRepository.clearRequesterInfoBefore(clock.now().minusYears(RETENTION_YEARS));
    }
}
