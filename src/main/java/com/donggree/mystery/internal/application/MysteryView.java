package com.donggree.mystery.internal.application;

import com.donggree.mystery.internal.application.readmodel.MysteryTodayReadModel;
import com.donggree.mystery.internal.domain.*;
import com.donggree.mystery.internal.domain.MysteryData.*;
import java.util.*;

/** 내부 정답·후보 집합·요청 ID는 공개 읽기 모델에 복사하지 않는다. */
final class MysteryView {
    private MysteryView() {}

    static MysteryTodayReadModel of(DailyMysteryCase daily, MysteryPlayer player) {
        var data = daily.getData();
        List<MysteryTodayReadModel.Attempt> attempts = new ArrayList<>();
        var source = player.getSession().attempts();
        for (int i = 0; i < source.size(); i++) {
            var a = source.get(i);
            var g = a.guess();
            attempts.add(new MysteryTodayReadModel.Attempt(
                    i + 1,
                    new MysteryTodayReadModel.Guess(g.departmentId(), g.buildingId(), g.methodId()),
                    a.correct(),
                    a.clue() == null ? null : a.clue().text(),
                    a.clue() == null ? null : a.clue().category().name()));
        }
        int games = player.getCompletedGames();
        var stats = new MysteryTodayReadModel.Statistics(
                games,
                player.getWonGames(),
                games == 0 ? 0 : 100.0 * player.getWonGames() / games,
                games == 0 ? null : (double) player.getTotalAttempts() / games,
                player.getCurrentStreak(),
                player.tier());
        var answer = player.finished()
                ? new MysteryTodayReadModel.Answer(
                        label(data.departments(), data.answer().departmentId()),
                        label(data.buildings(), data.answer().buildingId()),
                        label(data.methods(), data.answer().methodId()))
                : null;
        return new MysteryTodayReadModel(
                daily.getGameDate(),
                daily.getGameDate()
                        .plusDays(1)
                        .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
                        .toInstant(),
                player.getStatus().name(),
                options(data.departments()),
                options(data.buildings()),
                options(data.methods()),
                List.copyOf(attempts),
                stats,
                answer);
    }

    private static String label(List<Option> options, String id) {
        return options.stream()
                .filter(o -> o.id().equals(id))
                .findFirst()
                .orElseThrow()
                .label();
    }

    private static List<MysteryTodayReadModel.Option> options(List<Option> options) {
        return options.stream()
                .map(o -> new MysteryTodayReadModel.Option(o.id(), o.label()))
                .toList();
    }
}
