package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 장소 정보 수정 1회의 변경 전후 값. 이력 화면 없이 관리자가 DB에서 확인·복구한다.
 *
 * <p>메뉴는 차이 대신 전후 목록 전체를 저장한다. 장소와는 ID로만 연결하고,
 * 수정자는 회원 ID로만 참조하며 탈퇴 시 null로 익명화한다. 저장 후 바꾸지 않는다.
 */
@Entity
@Table(
        name = "place_edit_history",
        indexes = {
            @Index(name = "idx_place_edit_history_place_id", columnList = "place_id, edited_at"),
            @Index(name = "idx_place_edit_history_editor_id", columnList = "editor_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    /** 수정 회원 ID. 탈퇴 시 null로 익명화한다. */
    @Column(name = "editor_id")
    private Long editorId;

    @Column(name = "before_business_hours", length = Place.BUSINESS_HOURS_MAX_LENGTH)
    private String beforeBusinessHours;

    @Column(name = "after_business_hours", length = Place.BUSINESS_HOURS_MAX_LENGTH)
    private String afterBusinessHours;

    @Column(name = "before_description", length = Place.DESCRIPTION_MAX_LENGTH)
    private String beforeDescription;

    @Column(name = "after_description", length = Place.DESCRIPTION_MAX_LENGTH)
    private String afterDescription;

    // columnDefinition 없이 방언이 매핑하게 둔다(Postgres→jsonb, H2→json).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_menus", nullable = false)
    private List<MenuItem> beforeMenus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_menus", nullable = false)
    private List<MenuItem> afterMenus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_tags", nullable = false)
    private List<PlaceTag> beforeTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_tags", nullable = false)
    private List<PlaceTag> afterTags;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    PlaceEditHistory(Long placeId, Long editorId, PlaceInfo before, PlaceInfo after, LocalDateTime editedAt) {
        this.placeId = placeId;
        this.editorId = editorId;
        this.beforeBusinessHours = before.businessHours();
        this.afterBusinessHours = after.businessHours();
        this.beforeDescription = before.description();
        this.afterDescription = after.description();
        this.beforeMenus = before.menus();
        this.afterMenus = after.menus();
        this.beforeTags = before.tags();
        this.afterTags = after.tags();
        this.editedAt = editedAt;
    }

    public PlaceInfo before() {
        return new PlaceInfo(beforeBusinessHours, beforeDescription, beforeMenus, beforeTags);
    }

    public PlaceInfo after() {
        return new PlaceInfo(afterBusinessHours, afterDescription, afterMenus, afterTags);
    }
}
