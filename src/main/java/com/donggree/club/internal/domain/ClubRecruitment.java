package com.donggree.club.internal.domain;

import com.donggree.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 동아리의 기수별 모집 건. 동아리는 ID로만 참조한다.
 *
 * <p>가장 최근 건이 현재 모집이다. 진행 중·예정 모집은 고쳐 쓰고, 마감된 뒤 새로 올리면 새 건을 만든다(응용 서비스가 판단).
 */
@Entity
@Table(name = "club_recruitment", indexes = @Index(name = "idx_club_recruitment_club_id", columnList = "club_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubRecruitment extends BaseEntity {

    public static final int NOTICE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "generation")
    private Integer generation;

    @Column(name = "always_open", nullable = false)
    private boolean alwaysOpen;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "closed_early", nullable = false)
    private boolean closedEarly;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "links", nullable = false, columnDefinition = "jsonb")
    private List<ClubLink> links = new ArrayList<>();

    @Column(name = "notice", length = NOTICE_MAX_LENGTH)
    private String notice;

    private ClubRecruitment(Long clubId, RecruitmentPlan plan) {
        if (clubId == null || plan == null) {
            throw new IllegalArgumentException("동아리와 모집 정보가 필요합니다");
        }
        this.clubId = clubId;
        apply(plan);
    }

    public static ClubRecruitment open(Long clubId, RecruitmentPlan plan) {
        return new ClubRecruitment(clubId, plan);
    }

    /** 모집 정보 전체를 요청값으로 바꾼다. */
    public void revise(RecruitmentPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("모집 정보가 필요합니다");
        }
        apply(plan);
    }

    private void apply(RecruitmentPlan plan) {
        this.generation = plan.generation();
        this.alwaysOpen = plan.schedule().alwaysOpen();
        this.startsAt = plan.schedule().startsAt();
        this.endsAt = plan.schedule().endsAt();
        this.closedEarly = plan.schedule().closedEarly();
        this.links = new ArrayList<>(plan.links());
        this.notice = plan.notice();
    }

    public RecruitmentSchedule schedule() {
        return new RecruitmentSchedule(alwaysOpen, startsAt, endsAt, closedEarly);
    }

    /** 지금 모집 정보(요청 비교용 변경 전 값). */
    public RecruitmentPlan plan() {
        return new RecruitmentPlan(generation, schedule(), getLinks(), notice);
    }

    public List<ClubLink> getLinks() {
        return List.copyOf(links);
    }
}
