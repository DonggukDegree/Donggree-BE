package com.donggree.club.internal.application;

import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ClubImageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 동아리에 연결되지 않은 채 30일이 지났고 검토 중 요청에도 쓰이지 않는 동아리 사진을 지운다. 배너 사진은 지우지 않는다.
 * 행을 먼저 지우고 커밋한 뒤 파일을 지운다. 파일 삭제에 실패해도 남는 것은 참조 없는 파일뿐이다.
 */
@Service
@RequiredArgsConstructor
public class ClubImageCleanupService {

    static final int RETENTION_DAYS = 30;

    private final ClubImageRepository imageRepository;
    private final ClubImageStorage storage;
    private final ClubClock clock;
    private final TransactionTemplate transactionTemplate;

    /** @return 지운 사진 수 */
    public int deleteExpired() {
        List<String> names = transactionTemplate.execute(status -> {
            List<ClubImage> expired =
                    imageRepository.findExpiredUnattached(clock.now().minusDays(RETENTION_DAYS));
            imageRepository.deleteAll(expired);
            return expired.stream().map(ClubImage::getStoredName).toList();
        });
        if (names == null) return 0;
        names.forEach(storage::delete);
        return names.size();
    }
}
