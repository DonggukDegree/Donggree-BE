package com.donggree.mystery.internal.domain;

import com.donggree.mystery.internal.domain.MysteryData.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** 회차별 정보량 상한을 동일하게 적용하며, 9번째 오답 뒤에는 반드시 한 조합만 남긴다. */
public final class MysteryClues {
    private static final int[] LIMITS = {180, 120, 72, 48, 24, 12, 6, 3, 1};

    private MysteryClues() {}

    public static Clue generate(CaseData data, Session session, Guess guess, int wrongRound, Random random) {
        if (wrongRound < 1 || wrongRound > 9) throw new IllegalArgumentException("단서 생성 회차 오류");
        Category category = List.of(Category.values()).stream()
                .max(Comparator.comparingInt(c -> session.candidates(c).size()))
                .orElseThrow();
        List<String> current = session.candidates(category);
        if (current.size() <= 1) throw new IllegalStateException("이미 모든 후보가 확정되었습니다");
        int others = session.combinations() / current.size();
        int keep = Math.min(current.size() - 1, LIMITS[wrongRound - 1] / others);
        if (keep < 1) throw new IllegalStateException("풀이 가능성을 유지하지 못했습니다");
        String answer = data.answer().value(category);
        String submitted = guess.value(category);
        List<String> pool = new ArrayList<>(current);
        pool.remove(answer);
        // 제출한 값이 오답이면 이번 단서에서 직접 조사할 수 있도록 제외 그룹에 둔다.
        if (!submitted.equals(answer)) pool.remove(submitted);
        Collections.shuffle(pool, random);
        List<String> remaining = new ArrayList<>();
        remaining.add(answer);
        remaining.addAll(pool.subList(0, keep - 1));
        boolean included = submitted.equals(answer) || !current.contains(submitted) && random.nextBoolean();
        List<String> mentioned = included
                ? remaining
                : current.stream().filter(id -> !remaining.contains(id)).toList();
        // 포함형은 가능한 한 3~5개를 나열하되, 이미 제외된 선택지로 정보가 없는 단서를 만들지는 않는다.
        if (included && mentioned.size() < 3) {
            var padded = new ArrayList<>(mentioned);
            for (Option option : data.options(category)) {
                if (padded.size() >= 3) break;
                if (!current.contains(option.id())) padded.add(option.id());
            }
            mentioned = padded;
        }
        List<String> ids = List.copyOf(mentioned);
        String names = String.join(
                " · ",
                data.options(category).stream()
                        .filter(o -> ids.contains(o.id()))
                        .map(Option::label)
                        .toList());
        String subject =
                switch (category) {
                    case DEPARTMENT -> "범인의 소속 학과";
                    case BUILDING -> "연등을 가져간 장소";
                    case METHOD -> "사용한 수단";
                };
        String text = subject + (category == Category.METHOD ? "은" : "는") + (included ? " 다음 중 하나다: " : " 다음이 아니다: ")
                + names + ".";
        Clue clue = new Clue(category, included, ids, text);
        Session next = session.narrow(clue);
        if (!clue.test(data.answer())
                || next.combinations() >= session.combinations()
                || next.combinations() > LIMITS[wrongRound - 1]) throw new IllegalStateException("단서 불변식 위반");
        return clue;
    }
}
