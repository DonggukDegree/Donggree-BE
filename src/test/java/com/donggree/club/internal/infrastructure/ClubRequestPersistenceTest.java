package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditor;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubPhotos;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class ClubRequestPersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final ClubRequester REQUESTER = new ClubRequester(ClubRequesterRole.STAFF, "dongle@dgu.ac.kr");

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubRequestRepository requestRepository;

    @Autowired
    private ClubEditorRepository editorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 변경_전후_값을_날짜까지_jsonb로_보존한다() {
        Club club = saveClub();
        ClubRequest saved = requestRepository.saveAndFlush(ClubRequest.recruitment(
                club, 7L, REQUESTER, null, ClubRequestPayload.of(ClubFixtures.period(NOW, NOW.plusDays(7)))));
        Long infoId = requestRepository
                .saveAndFlush(ClubRequest.edit(
                        club,
                        7L,
                        REQUESTER,
                        ClubRequestPayload.of(club.profile(), ClubPhotos.NONE),
                        ClubRequestPayload.of(ClubFixtures.profile("소리마루 밴드"), new ClubPhotos(List.of(3L, 4L), 4L))))
                .getId();
        entityManager.clear();

        ClubRequest recruitment = requestRepository.findById(saved.getId()).orElseThrow();
        ClubRequest info = requestRepository.findById(infoId).orElseThrow();

        assertThat(recruitment.getAfter().recruitment().schedule().endsAt()).isEqualTo(NOW.plusDays(7));
        assertThat(recruitment.getAfter().recruitment().links()).hasSize(1);
        assertThat(info.getBefore().profile().activityStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(info.getAfter().profile().officers().get(0).name()).isEqualTo("김동글");
        assertThat(info.getAfter().photos()).isEqualTo(new ClubPhotos(List.of(3L, 4L), 4L));
        assertThat(requestRepository.findByRequesterIdOrderByIdDesc(7L))
                .extracting(ClubRequest::getId)
                .containsExactly(infoId, saved.getId());
    }

    @Test
    void 회원당_같은_동아리_종류의_검토_중_요청은_하나다() {
        Club club = saveClub();
        requestRepository.saveAndFlush(editRequest(club));

        assertThatThrownBy(() -> requestRepository.saveAndFlush(editRequest(club)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 보유_기간이_지난_연락처와_관계를_비우고_탈퇴하면_요청자_연결을_끊는다() {
        Club club = saveClub();
        Long oldId = requestRepository.saveAndFlush(editRequest(club)).getId();
        Long newId = requestRepository
                .saveAndFlush(ClubRequest.register(
                        8L, REQUESTER, ClubRequestPayload.of(ClubFixtures.profile("가"), ClubPhotos.NONE)))
                .getId();
        entityManager
                .createNativeQuery("UPDATE club_request SET created_at = :at WHERE id = :id")
                .setParameter("at", NOW.minusYears(1).minusDays(1))
                .setParameter("id", oldId)
                .executeUpdate();

        assertThat(requestRepository.clearRequesterInfoBefore(NOW.minusYears(1)))
                .isEqualTo(1);
        assertThat(requestRepository.clearRequester(8L)).isEqualTo(1);

        ClubRequest expired = requestRepository.findById(oldId).orElseThrow();
        ClubRequest withdrawn = requestRepository.findById(newId).orElseThrow();
        assertThat(expired.getRequesterContact()).isNull();
        assertThat(expired.getRequesterRole()).isNull();
        assertThat(expired.getRequesterId()).isEqualTo(7L);
        assertThat(withdrawn.getRequesterId()).isNull();
        assertThat(withdrawn.getRequesterContact()).isNull();
    }

    @Test
    void 직접_수정_권한을_확인하고_탈퇴하면_지운다() {
        Club club = saveClub();
        editorRepository.saveAndFlush(ClubEditor.grant(club.getId(), 7L, NOW));

        assertThat(editorRepository.canEdit(club.getId(), 7L)).isTrue();
        assertThat(editorRepository.canEdit(club.getId(), 8L)).isFalse();
        assertThat(editorRepository.canEdit(club.getId(), null)).isFalse();
        assertThat(editorRepository.deleteByMember(7L)).isEqualTo(1);
        assertThat(editorRepository.canEdit(club.getId(), 7L)).isFalse();
    }

    private Club saveClub() {
        return clubRepository.saveAndFlush(Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, NOW));
    }

    private static ClubRequest editRequest(Club club) {
        return ClubRequest.edit(
                club,
                7L,
                REQUESTER,
                ClubRequestPayload.of(club.profile(), ClubPhotos.NONE),
                ClubRequestPayload.of(ClubFixtures.profile("소리마루 밴드"), ClubPhotos.NONE));
    }
}
