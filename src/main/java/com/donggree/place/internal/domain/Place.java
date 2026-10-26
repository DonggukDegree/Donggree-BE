package com.donggree.place.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동글맵 장소. 인증 회원이 지도 중심 핀으로 직접 등록한다.
 *
 * <p>이름·위치·카테고리는 등록 후 사용자가 바꿀 수 없고 장소는 삭제하지 않는다.
 * 카카오 응답은 저장하지 않으며 좌표는 사용자가 지정한 값, 주소는 표시 시점에 FE가 조회한다.
 * 작성자는 회원 ID로만 참조하고 탈퇴 시 연결을 끊는다.
 */
@Entity
@Table(
        name = "place",
        indexes = {
            @Index(name = "idx_place_status_location", columnList = "status, latitude, longitude"),
            @Index(name = "idx_place_created_by", columnList = "created_by")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

    public static final int NAME_MAX_LENGTH = 30;
    public static final int BUSINESS_HOURS_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 300;
    public static final int MENU_MAX_COUNT = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PlaceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_category", nullable = false, length = 30)
    private PlaceSubCategory subCategory;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "business_hours", length = BUSINESS_HOURS_MAX_LENGTH)
    private String businessHours;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PlaceStatus status;

    /** 공동 편집 충돌 감지용 버전. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** 최초 등록 회원 ID. 탈퇴 시 null로 익명화한다. */
    @Column(name = "created_by")
    private Long createdBy;

    /** 사용자에게 보여주는 마지막 정보 수정 시각. 숨김·익명화 같은 관리 변경으로는 바뀌지 않는다. */
    @Column(name = "info_updated_at", nullable = false)
    private LocalDateTime infoUpdatedAt;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PlaceMenu> menus = new ArrayList<>();

    private Place(PlaceRegistration registration) {
        if (registration.memberId() == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        this.name = requireName(registration.name());
        assignCategory(registration.category(), registration.subCategory());
        assignLocation(registration.point());
        this.businessHours = optionalText(registration.businessHours(), BUSINESS_HOURS_MAX_LENGTH, "businessHours");
        this.description = optionalText(registration.description(), DESCRIPTION_MAX_LENGTH, "description");
        replaceMenus(registration.menus());
        this.status = PlaceStatus.VISIBLE;
        this.createdBy = registration.memberId();
        this.infoUpdatedAt = LocalDateTime.now();
    }

    public static Place register(PlaceRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must not be null");
        }
        return new Place(registration);
    }

    public GeoPoint point() {
        return new GeoPoint(latitude, longitude);
    }

    public boolean isVisible() {
        return status == PlaceStatus.VISIBLE;
    }

    public List<PlaceMenu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    private void replaceMenus(List<MenuItem> items) {
        List<MenuItem> source = items == null ? List.of() : items;
        if (source.size() > MENU_MAX_COUNT) {
            throw new IllegalArgumentException("menus must be " + MENU_MAX_COUNT + " or fewer");
        }
        menus.clear();
        for (int index = 0; index < source.size(); index++) {
            menus.add(new PlaceMenu(this, source.get(index), index));
        }
    }

    private void assignCategory(PlaceCategory category, PlaceSubCategory subCategory) {
        if (category == null || subCategory == null) {
            throw new IllegalArgumentException("category and subCategory must not be null");
        }
        if (!subCategory.belongsTo(category)) {
            throw new IllegalArgumentException("subCategory must belong to category");
        }
        this.category = category;
        this.subCategory = subCategory;
    }

    private void assignLocation(GeoPoint point) {
        if (point == null) {
            throw new IllegalArgumentException("point must not be null");
        }
        if (!CampusArea.contains(point)) {
            throw new IllegalArgumentException("point must be inside the campus area");
        }
        this.latitude = point.latitude();
        this.longitude = point.longitude();
    }

    private static String requireName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name must be 1 to " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    /** 공백만 입력한 선택 항목은 미입력으로 저장한다. */
    private static String optionalText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
        return trimmed;
    }
}
