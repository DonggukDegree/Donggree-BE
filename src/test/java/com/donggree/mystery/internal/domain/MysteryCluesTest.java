package com.donggree.mystery.internal.domain;

import static org.assertj.core.api.Assertions.*;

import com.donggree.mystery.internal.domain.MysteryData.*;
import com.donggree.mystery.internal.infrastructure.MysteryCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;

class MysteryCluesTest {
    private List<Option> options(String prefix) {
        return java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> new Option(prefix + i, prefix + i))
                .toList();
    }

    @Test
    void 모든_정답과_임의_추측에서_단서가_참이고_9회_안에_유일해진다() {
        var random = new Random(1120);
        for (int d = 0; d < 6; d++)
            for (int b = 0; b < 6; b++)
                for (int m = 0; m < 6; m++)
                    for (int path = 0; path < 12; path++) {
                        var answer = new Guess("D" + d, "B" + b, "M" + m);
                        var data = new CaseData(options("D"), options("B"), options("M"), answer);
                        var session = Session.start(data);
                        Set<Guess> used = new HashSet<>();
                        for (int round = 1; round <= 9; round++) {
                            Guess guess;
                            do {
                                guess = new Guess(
                                        "D" + random.nextInt(6), "B" + random.nextInt(6), "M" + random.nextInt(6));
                            } while (guess.equals(answer) || !used.add(guess));
                            Clue clue = MysteryClues.generate(data, session, guess, round, random);
                            assertThat(clue.test(answer)).isTrue();
                            var next = session.narrow(clue);
                            assertThat(next.combinations()).isLessThan(session.combinations());
                            for (Category c : Category.values())
                                assertThat(next.candidates(c)).contains(answer.value(c));
                            if (session.candidates(clue.category()).contains(guess.value(clue.category())))
                                assertThat(clue.optionIds()).contains(guess.value(clue.category()));
                            session = next.append(new Attempt(UUID.randomUUID(), guess, false, clue));
                        }
                        assertThat(session.combinations()).isEqualTo(1);
                    }
    }

    @Test
    void 카탈로그는_중복없이_6개이며_정답을_포함한다() {
        var catalog = new MysteryCatalog();
        for (int i = 0; i < 500; i++) {
            var data = catalog.generate();
            assertThat(data.contains(data.answer())).isTrue();
            for (Category c : Category.values())
                assertThat(data.options(c)).hasSize(6).doesNotHaveDuplicates();
        }
    }
}
