package com.donggree.club.internal.application;

import com.donggree.club.internal.application.readmodel.ClubListRow;
import com.donggree.club.internal.application.readmodel.ClubPageReadModel;
import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import com.donggree.club.internal.domain.RecruitmentSelection;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 동아리 목록의 필터·정렬·커서 계산. 모집 상태·마감 임박은 조회 시각에 따라 달라져 저장 값으로 정렬할 수 없으므로,
 * 전체 동아리를 읽어 여기서 계산한다(교내 동아리 규모라 한 번에 읽는다).
 */
final class ClubCatalog {

    static final int PAGE_SIZE = 12;

    private ClubCatalog() {}

    /** @param after 이전 묶음의 마지막 위치. null이면 처음부터 */
    static ClubPageReadModel page(List<ClubListRow> rows, ClubSearch search, ClubCursor after, LocalDateTime now) {
        Collator collator = Collator.getInstance(Locale.KOREAN);
        Comparator<ClubCursor> order = Comparator.comparingInt(ClubCursor::group)
                .thenComparingLong(ClubCursor::value)
                .thenComparing(ClubCursor::name, collator)
                .thenComparingLong(ClubCursor::id);

        List<Entry> matched = rows.stream()
                .map(row -> toEntry(row, search.sort(), now))
                .filter(entry -> matches(entry, search, now))
                .sorted(Comparator.comparing(Entry::key, order))
                .toList();
        List<Entry> rest = after == null
                ? matched
                : matched.stream()
                        .filter(entry -> order.compare(entry.key(), after) > 0)
                        .toList();
        List<Entry> page = rest.stream().limit(PAGE_SIZE).toList();
        String nextCursor =
                rest.size() > PAGE_SIZE ? page.get(page.size() - 1).key().encode() : null;
        return new ClubPageReadModel(page.stream().map(Entry::summary).toList(), nextCursor, matched.size());
    }

    private static Entry toEntry(ClubListRow row, ClubSort sort, LocalDateTime now) {
        ClubRecruitmentReadModel latest = row.latestRecruitment();
        ClubRecruitmentReadModel current = RecruitmentSelection.of(
                        latest, latest == null ? null : latest.schedule(), row.activityEndDate(), now)
                .current();
        ClubSummaryReadModel summary = new ClubSummaryReadModel(
                row.id(),
                row.name(),
                row.affiliation(),
                row.fields(),
                row.registration(),
                row.activity().confirmedOn(now.toLocalDate()),
                List.of(),
                row.totalMemberCount(),
                row.foundedDate(),
                current);
        return new Entry(summary, key(summary, sort, now));
    }

    private static boolean matches(Entry entry, ClubSearch search, LocalDateTime now) {
        ClubSummaryReadModel club = entry.summary();
        if (search.ids() != null && !search.ids().contains(club.id())) return false;
        if (!search.affiliations().isEmpty() && !search.affiliations().contains(club.affiliation())) return false;
        if (!search.fields().isEmpty() && club.fields().stream().noneMatch(search.fields()::contains)) return false;
        return search.recruitmentStatuses().isEmpty()
                || search.recruitmentStatuses().contains(status(club.recruitment(), now));
    }

    static RecruitmentStatus status(ClubRecruitmentReadModel recruitment, LocalDateTime now) {
        return recruitment == null
                ? RecruitmentStatus.BEFORE
                : recruitment.schedule().statusAt(now);
    }

    private static ClubCursor key(ClubSummaryReadModel club, ClubSort sort, LocalDateTime now) {
        return switch (sort) {
            case NAME -> new ClubCursor(0, 0, club.name(), club.id());
            case MEMBERS -> club.totalMemberCount() == null
                    ? new ClubCursor(1, 0, club.name(), club.id())
                    : new ClubCursor(0, -club.totalMemberCount(), club.name(), club.id());
            case OLDEST -> club.foundedDate() == null
                    ? new ClubCursor(1, 0, club.name(), club.id())
                    : new ClubCursor(0, club.foundedDate().toEpochDay(), club.name(), club.id());
            case DEADLINE -> deadlineKey(club, now);
        };
    }

    // 기간 모집 중(마감 빠른 순) → 상시모집 → 나머지(모집 전·마감).
    private static ClubCursor deadlineKey(ClubSummaryReadModel club, LocalDateTime now) {
        ClubRecruitmentReadModel recruitment = club.recruitment();
        if (status(recruitment, now) != RecruitmentStatus.OPEN) return new ClubCursor(2, 0, club.name(), club.id());
        LocalDateTime deadline = recruitment.schedule().openDeadlineAt(now);
        if (deadline == null) return new ClubCursor(1, 0, club.name(), club.id());
        return new ClubCursor(0, deadline.toEpochSecond(ZoneOffset.UTC), club.name(), club.id());
    }

    private record Entry(ClubSummaryReadModel summary, ClubCursor key) {}
}
