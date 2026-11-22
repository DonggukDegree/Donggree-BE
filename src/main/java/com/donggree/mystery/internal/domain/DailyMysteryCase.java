package com.donggree.mystery.internal.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 날짜별 문제는 최초 생성 이후 재추첨하거나 수정하지 않는다. */
@Entity
@Table(name = "daily_mystery_case")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyMysteryCase {
    @Id
    @Column(name = "game_date")
    private LocalDate gameDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "case_data", nullable = false, columnDefinition = "jsonb")
    private MysteryData.CaseData data;
}
