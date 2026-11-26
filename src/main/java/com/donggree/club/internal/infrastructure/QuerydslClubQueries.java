package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubQueries;
import com.donggree.club.internal.application.readmodel.ClubListRow;
import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.QClub;
import com.donggree.club.internal.domain.QClubRecruitment;
import com.donggree.club.internal.domain.enums.ClubField;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** {@link ClubQueries}의 QueryDSL 구현. 소개·운영진 같은 상세 열은 읽지 않고, 모집 건은 동아리별 최근 건만 읽는다. */
@Repository
@RequiredArgsConstructor
public class QuerydslClubQueries implements ClubQueries {

    private static final QClub CLUB = QClub.club;
    private static final QClubRecruitment RECRUITMENT = QClubRecruitment.clubRecruitment;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ClubListRow> findListRows() {
        Map<Long, List<ClubField>> fields = findFields();
        Map<Long, ClubRecruitmentReadModel> recruitments = findLatestRecruitments();
        return queryFactory
                .select(
                        CLUB.id,
                        CLUB.name,
                        CLUB.affiliation,
                        CLUB.registration,
                        CLUB.generation,
                        CLUB.activityStartDate,
                        CLUB.activityEndDate,
                        CLUB.infoUpdatedAt,
                        CLUB.totalMemberCount,
                        CLUB.foundedDate)
                .from(CLUB)
                .orderBy(CLUB.id.asc())
                .fetch()
                .stream()
                .map(row -> new ClubListRow(
                        row.get(CLUB.id),
                        row.get(CLUB.name),
                        row.get(CLUB.affiliation),
                        fields.getOrDefault(row.get(CLUB.id), List.of()),
                        row.get(CLUB.registration),
                        row.get(CLUB.generation),
                        row.get(CLUB.activityStartDate),
                        row.get(CLUB.activityEndDate),
                        row.get(CLUB.infoUpdatedAt),
                        row.get(CLUB.totalMemberCount),
                        row.get(CLUB.foundedDate),
                        recruitments.get(row.get(CLUB.id))))
                .toList();
    }

    // 동아리별 활동 분야(enum 순서).
    private Map<Long, List<ClubField>> findFields() {
        EnumPath<ClubField> field = Expressions.enumPath(ClubField.class, "field");
        return queryFactory.select(CLUB.id, field).from(CLUB).join(CLUB.fields, field).fetch().stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(CLUB.id),
                        Collectors.collectingAndThen(
                                Collectors.mapping(row -> row.get(field), Collectors.toList()),
                                list -> list.stream().sorted().toList())));
    }

    private Map<Long, ClubRecruitmentReadModel> findLatestRecruitments() {
        QClubRecruitment sub = new QClubRecruitment("sub");
        return queryFactory
                .selectFrom(RECRUITMENT)
                .where(RECRUITMENT.id.in(
                        JPAExpressions.select(sub.id.max()).from(sub).groupBy(sub.clubId)))
                .fetch()
                .stream()
                .collect(Collectors.toMap(ClubRecruitment::getClubId, ClubRecruitmentReadModel::from));
    }
}
