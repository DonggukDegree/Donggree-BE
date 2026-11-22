package com.donggree.mystery.internal.application.readmodel;

import java.time.*;
import java.util.List;

/** HTTP DTO와 별개로 오늘의 공개 가능한 정보만 담는다. */
public record MysteryTodayReadModel(
        LocalDate gameDate,
        Instant nextResetAt,
        String status,
        List<Option> departments,
        List<Option> buildings,
        List<Option> methods,
        List<Attempt> attempts,
        Statistics statistics,
        Answer answer) {
    public record Option(String id, String label) {}

    public record Guess(String departmentId, String buildingId, String methodId) {}

    public record Attempt(int number, Guess guess, boolean correct, String clueText, String clueCategory) {}

    public record Statistics(
            int completedGames,
            int wonGames,
            double successRate,
            Double averageAttempts,
            int currentStreak,
            String tier) {}

    public record Answer(String department, String building, String method) {}
}
