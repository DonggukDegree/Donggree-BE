package com.donggree.club.internal.application.admin;

import com.donggree.club.internal.application.ClubCatalog;
import com.donggree.club.internal.application.ClubClock;
import com.donggree.club.internal.application.ClubQueries;
import com.donggree.club.internal.application.readmodel.ClubListRow;
import com.donggree.club.internal.domain.ClubEditor;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.user.UserModerationService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 동아리 요청·동아리 조회. 요청자·권한 회원은 실명과 회원 ID로 보여준다. */
@Service
@RequiredArgsConstructor
public class AdminClubQueryService {

    private final ClubRequestRepository requestRepository;
    private final ClubEditorRepository editorRepository;
    private final ClubQueries clubQueries;
    private final UserModerationService userModerationService;
    private final ClubClock clock;

    /**
     * 검토 중은 먼저 들어온 순(대기 순서, 다시 보낸 요청은 맨 뒤), 그 밖의 상태와 전체는 최신순.
     *
     * @param status null이면 전체
     */
    @Transactional(readOnly = true)
    public List<AdminClubRequestReadModel> getRequests(ClubRequestStatus status) {
        List<ClubRequest> requests = status == null
                ? requestRepository.findAllByOrderByIdDesc()
                : status == ClubRequestStatus.PENDING
                        ? requestRepository.findByStatusOrderByIdAsc(status)
                        : requestRepository.findByStatusOrderByIdDesc(status);
        Map<Long, String> names =
                names(requests.stream().map(ClubRequest::getRequesterId).toList());
        return requests.stream()
                .map(request -> AdminClubRequestReadModel.of(request, names.get(request.getRequesterId())))
                .toList();
    }

    /** 먼저 등록된 순(ID순). keyword는 이름 부분 일치이며 비우면 전체. */
    @Transactional(readOnly = true)
    public List<AdminClubReadModel> getClubs(String keyword) {
        String query = keyword == null ? "" : keyword.strip();
        LocalDateTime now = clock.now();
        List<ClubListRow> rows = clubQueries.findListRows().stream()
                .filter(row -> query.isEmpty() || row.name().contains(query))
                .toList();
        List<ClubEditor> editors = rows.isEmpty()
                ? List.of()
                : editorRepository.findByClubIds(
                        rows.stream().map(ClubListRow::id).toList());
        Map<Long, String> names = names(
                editors.stream().map(editor -> editor.getKey().getMemberId()).toList());
        Map<Long, List<AdminClubReadModel.Editor>> editorsByClub = editors.stream()
                .collect(Collectors.groupingBy(
                        editor -> editor.getKey().getClubId(),
                        Collectors.mapping(
                                editor -> new AdminClubReadModel.Editor(
                                        editor.getKey().getMemberId(),
                                        names.get(editor.getKey().getMemberId()),
                                        editor.getGrantedAt()),
                                Collectors.toList())));
        return rows.stream()
                .map(row -> new AdminClubReadModel(
                        ClubCatalog.summarize(row, now),
                        editorsByClub.getOrDefault(row.id(), List.of()),
                        row.infoUpdatedAt()))
                .toList();
    }

    private Map<Long, String> names(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userModerationService.findAdminProfiles(ids).entrySet().stream()
                .filter(entry -> entry.getValue().name() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().name()));
    }
}
