package com.donggree.club.internal.application;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.ClubFavorite;
import com.donggree.club.internal.domain.ClubFavoriteRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 동아리 찜. 로그인 회원 누구나 쓰며, 찜·찜 해제는 여러 번 보내도 결과가 같다(멱등). 찜 목록 조회와 응집해 한 서비스로 둔다. */
@Service
@RequiredArgsConstructor
public class ClubFavoriteService {

    private final ClubFavoriteRepository favoriteRepository;
    private final ClubRepository clubRepository;
    private final ClubClock clock;

    @Transactional(readOnly = true)
    public List<Long> getFavoriteClubIds(Long memberId) {
        return favoriteRepository.findClubIds(memberId);
    }

    @Transactional
    public void add(Long memberId, Long clubId) {
        if (!clubRepository.existsById(clubId)) throw new GeneralException(ClubErrorCode.CLUB_NOT_FOUND);
        if (favoriteRepository.existsById(new ClubFavorite.Key(memberId, clubId))) return;
        try {
            favoriteRepository.saveAndFlush(ClubFavorite.of(memberId, clubId, clock.now()));
        } catch (DataIntegrityViolationException exception) {
            // 같은 찜이 동시에 들어오면 먼저 저장된 것을 그대로 둔다.
        }
    }

    @Transactional
    public void remove(Long memberId, Long clubId) {
        favoriteRepository.deleteById(new ClubFavorite.Key(memberId, clubId));
    }
}
