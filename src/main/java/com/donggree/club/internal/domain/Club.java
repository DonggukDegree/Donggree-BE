package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 동아리. 관리자 임시 등록 또는 회원 신규 등록 요청 승인으로 생긴다.
 *
 * <p>정보 수정은 정보 전체를 교체하며 마지막 정보 수정 시각을 갱신한다. 임시 등록 동아리는 회원 요청이 처음 승인되면 정식 등록으로 바뀐다.
 * 모집 건은 {@link ClubRecruitment}가 동아리 ID로 따로 가진다.
 */
@Entity
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = ClubProfile.NAME_MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "affiliation", nullable = false, length = 20)
    private ClubAffiliation affiliation;

    @ElementCollection
    @CollectionTable(name = "club_field", joinColumns = @JoinColumn(name = "club_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 30)
    private Set<ClubField> fields = new HashSet<>();

    @Column(name = "description", nullable = false, length = ClubProfile.DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "generation")
    private Integer generation;

    @Column(name = "activity_start_date")
    private LocalDate activityStartDate;

    @Column(name = "activity_end_date")
    private LocalDate activityEndDate;

    @Column(name = "room_location", length = ClubProfile.ROOM_MAX_LENGTH)
    private String roomLocation;

    @Column(name = "generation_member_count")
    private Integer generationMemberCount;

    @Column(name = "total_member_count")
    private Integer totalMemberCount;

    @Column(name = "founded_date")
    private LocalDate foundedDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "officers", nullable = false, columnDefinition = "jsonb")
    private List<ClubOfficer> officers = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "links", nullable = false, columnDefinition = "jsonb")
    private List<ClubLink> links = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "registration", nullable = false, length = 15)
    private ClubRegistration registration;

    /** 동시 수정 충돌 감지용 버전. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** 화면에 보여주는 마지막 정보 등록·수정 시각. 등록 구분 전환 같은 관리 변경으로는 바뀌지 않는다. */
    @Column(name = "info_updated_at", nullable = false)
    private LocalDateTime infoUpdatedAt;

    private Club(ClubProfile profile, ClubRegistration registration, LocalDateTime now) {
        if (profile == null || registration == null || now == null) {
            throw new IllegalArgumentException("동아리 정보와 등록 구분이 필요합니다");
        }
        this.registration = registration;
        apply(profile, now);
    }

    public static Club register(ClubProfile profile, ClubRegistration registration, LocalDateTime now) {
        return new Club(profile, registration, now);
    }

    /** 정보 전체를 요청값으로 바꾸고 마지막 정보 수정 시각을 갱신한다. */
    public void edit(ClubProfile profile, LocalDateTime now) {
        if (profile == null || now == null) {
            throw new IllegalArgumentException("동아리 정보가 필요합니다");
        }
        apply(profile, now);
    }

    private void apply(ClubProfile profile, LocalDateTime now) {
        this.name = profile.name();
        this.affiliation = profile.affiliation();
        this.fields.clear();
        this.fields.addAll(profile.fields());
        this.description = profile.description();
        this.generation = profile.generation();
        this.activityStartDate = profile.activityStartDate();
        this.activityEndDate = profile.activityEndDate();
        this.roomLocation = profile.roomLocation();
        this.generationMemberCount = profile.generationMemberCount();
        this.totalMemberCount = profile.totalMemberCount();
        this.foundedDate = profile.foundedDate();
        this.officers = new ArrayList<>(profile.officers());
        this.links = new ArrayList<>(profile.links());
        this.infoUpdatedAt = now;
    }

    /** 활동 분야(enum 순서). */
    public List<ClubField> sortedFields() {
        return fields.stream().sorted(Comparator.naturalOrder()).toList();
    }

    public List<ClubOfficer> getOfficers() {
        return List.copyOf(officers);
    }

    public List<ClubLink> getLinks() {
        return List.copyOf(links);
    }

    /** 지금 정보(요청 비교용 변경 전 값). */
    public ClubProfile profile() {
        return new ClubProfile(
                name,
                affiliation,
                sortedFields(),
                description,
                generation,
                activityStartDate,
                activityEndDate,
                roomLocation,
                generationMemberCount,
                totalMemberCount,
                foundedDate,
                getOfficers(),
                getLinks());
    }

    public ClubActivity activity() {
        return new ClubActivity(generation, activityStartDate, activityEndDate, infoUpdatedAt);
    }

    /** 아직 회원 최초 등록자가 없는 동아리(관리자 임시 등록)면 첫 회원 승인 때 최초 등록 포인트 대상이다. */
    public boolean firstRegistrationRewardAvailable() {
        return registration == ClubRegistration.PROVISIONAL;
    }
}
