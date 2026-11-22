package com.donggree.mystery.internal.presentation.dto;

import com.donggree.mystery.internal.application.readmodel.MysteryTodayReadModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.*;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MysteryTodayResponse(
        LocalDate gameDate,
        Instant nextResetAt,
        String title,
        String subtitle,
        int maxAttempts,
        String status,
        Options options,
        List<Attempt> attempts,
        Statistics statistics,
        Answer answer) {
    public record Option(String id, String label) {}

    public record Options(List<Option> departments, List<Option> buildings, List<Option> methods) {}

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

    public static MysteryTodayResponse from(MysteryTodayReadModel view) {
        var s = view.statistics();
        var a = view.answer();
        return new MysteryTodayResponse(
                view.gameDate(),
                view.nextResetAt(),
                "정각원의 비밀",
                "사라진 연등",
                10,
                view.status(),
                new Options(options(view.departments()), options(view.buildings()), options(view.methods())),
                view.attempts().stream()
                        .map(t -> new Attempt(
                                t.number(),
                                new Guess(
                                        t.guess().departmentId(),
                                        t.guess().buildingId(),
                                        t.guess().methodId()),
                                t.correct(),
                                t.clueText(),
                                t.clueCategory()))
                        .toList(),
                new Statistics(
                        s.completedGames(),
                        s.wonGames(),
                        s.successRate(),
                        s.averageAttempts(),
                        s.currentStreak(),
                        s.tier()),
                a == null ? null : new Answer(a.department(), a.building(), a.method()));
    }

    private static List<Option> options(List<MysteryTodayReadModel.Option> values) {
        return values.stream().map(v -> new Option(v.id(), v.label())).toList();
    }
}
