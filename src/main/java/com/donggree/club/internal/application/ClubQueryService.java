package com.donggree.club.internal.application;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubDetailReadModel;
import com.donggree.club.internal.application.readmodel.ClubPageReadModel;
import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.RecruitmentSelection;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 동아리 쇼핑 조회(Query) 응용 서비스. 로그인 회원 누구나 조회한다. */
@Service
@RequiredArgsConstructor
public class ClubQueryService {

    private final ClubQueries clubQueries;
    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository recruitmentRepository;
    private final ClubClock clock;

    /** 필터·정렬을 적용한 목록을 12개씩 준다. */
    @Transactional(readOnly = true)
    public ClubPageReadModel getClubs(ClubSearch search, String cursor) {
        return ClubCatalog.page(clubQueries.findListRows(), search, decode(cursor), clock.now());
    }

    private static ClubCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return ClubCursor.decode(cursor);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_CURSOR);
        }
    }

    @Transactional(readOnly = true)
    public ClubDetailReadModel getClub(Long clubId) {
        Club club =
                clubRepository.findById(clubId).orElseThrow(() -> new GeneralException(ClubErrorCode.CLUB_NOT_FOUND));
        LocalDateTime now = clock.now();
        ClubRecruitmentReadModel latest = recruitmentRepository
                .findTopByClubIdOrderByIdDesc(clubId)
                .map(ClubRecruitmentReadModel::from)
                .orElse(null);
        RecruitmentSelection<ClubRecruitmentReadModel> selection = RecruitmentSelection.of(
                latest, latest == null ? null : latest.schedule(), club.getActivityEndDate(), now);
        // 직접 수정 권한은 요청 승인 API에서, 사진은 사진 저장 API에서 채운다.
        return new ClubDetailReadModel(
                club.getId(),
                club.getName(),
                club.getAffiliation(),
                club.sortedFields(),
                club.getRegistration(),
                club.activity().confirmedOn(now.toLocalDate()),
                club.getTotalMemberCount(),
                club.getFoundedDate(),
                selection.current(),
                club.getDescription(),
                club.getGeneration(),
                club.getActivityStartDate(),
                club.getActivityEndDate(),
                club.getRoomLocation(),
                club.getGenerationMemberCount(),
                List.of(),
                null,
                selection.previous(),
                club.getOfficers(),
                club.getLinks(),
                false,
                club.firstRegistrationRewardAvailable(),
                club.getInfoUpdatedAt());
    }
}
