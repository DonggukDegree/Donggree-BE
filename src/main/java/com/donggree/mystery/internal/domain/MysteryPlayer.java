package com.donggree.mystery.internal.domain;

import com.donggree.mystery.internal.domain.MysteryData.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 오늘의 기록과 이번 연속 참여 기간의 통계만 보관한다. 이전 날짜별 결과는 덮어쓴다. */
@Entity
@Table(name = "mystery_player")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MysteryPlayer {
    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "game_date")
    private LocalDate gameDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_STARTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Session session = Session.empty();

    // 성공 또는 10회 실패로 마지막 판을 완료한 날짜.
    @Column(name = "last_played_date")
    private LocalDate lastPlayedDate;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "completed_games", nullable = false)
    private int completedGames;

    @Column(name = "won_games", nullable = false)
    private int wonGames;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    public void openDay(LocalDate today) {
        if (today.equals(gameDate)) return;
        // 완료한 날만 참여일이다. 미완료 또는 미참여 날짜를 건너뛰면 기존 통계를 초기화한다.
        if (lastPlayedDate != null && lastPlayedDate.isBefore(today.minusDays(1))) resetStatistics();
        gameDate = today;
        status = Status.NOT_STARTED;
        session = Session.empty();
    }

    private void resetStatistics() {
        currentStreak = 0;
        completedGames = 0;
        wonGames = 0;
        totalAttempts = 0;
        lastPlayedDate = null;
    }

    public void start(CaseData data) {
        if (status != Status.NOT_STARTED) return;
        session = Session.start(data);
        status = Status.IN_PROGRESS;
    }

    public void record(Attempt attempt) {
        if (status != Status.IN_PROGRESS) throw new IllegalStateException("진행 중인 판에만 추리를 기록할 수 있습니다");
        if (attempt.clue() != null) session = session.narrow(attempt.clue());
        session = session.append(attempt);
        if (attempt.correct() || session.attempts().size() == 10) {
            status = attempt.correct() ? Status.WON : Status.LOST;
            currentStreak++;
            lastPlayedDate = gameDate;
            completedGames++;
            totalAttempts += session.attempts().size();
            if (attempt.correct()) wonGames++;
        }
    }

    public boolean finished() {
        return status == Status.WON || status == Status.LOST;
    }

    public String tier() {
        return currentStreak >= 30
                ? "DIAMOND"
                : currentStreak >= 14 ? "GOLD" : currentStreak >= 7 ? "SILVER" : "BRONZE";
    }
}
