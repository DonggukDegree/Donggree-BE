package com.donggree.mystery.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.mystery.internal.application.command.MysteryGuessCommand;
import com.donggree.mystery.internal.domain.*;
import com.donggree.mystery.internal.domain.MysteryData.*;
import com.donggree.mystery.internal.infrastructure.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;

@DataJpaTest
@Import({MysteryCaseService.class, MysteryQueryService.class, MysteryCommandService.class, MysteryCatalog.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MysteryPersistenceTest {
    @Autowired
    MysteryQueryService queries;

    @Autowired
    MysteryCommandService commands;

    @Autowired
    DailyMysteryCaseRepository cases;

    @Autowired
    MysteryPlayerRepository players;

    @MockitoBean
    MysteryCalendar clock;

    final LocalDate day = LocalDate.of(2026, 11, 19);

    @BeforeEach
    void setup() {
        given(clock.today()).willReturn(day);
    }

    @AfterEach
    void clean() {
        players.deleteAll();
        cases.deleteAll();
    }

    private MysteryGuessCommand command(Guess guess) {
        return new MysteryGuessCommand(day, UUID.randomUUID(), guess);
    }

    private Guess answer() {
        return cases.findById(day).orElseThrow().getData().answer();
    }

    private Guess wrong() {
        var a = answer();
        var data = cases.findById(day).orElseThrow().getData();
        return new Guess(
                data.departments().stream()
                        .filter(o -> !o.id().equals(a.departmentId()))
                        .findFirst()
                        .orElseThrow()
                        .id(),
                a.buildingId(),
                a.methodId());
    }

    @Test
    void 모든_회원은_같은_문제를_보고_인증_상태와_관계없이_저장된다() {
        var a = queries.today(1L);
        var b = queries.today(2L);
        assertThat(cases.count()).isEqualTo(1);
        assertThat(a.departments()).isEqualTo(b.departments());
        assertThat(a.answer()).isNull();
        assertThat(a.statistics().currentStreak()).isZero();
    }

    @Test
    void 정답과_재접속_결과를_복원하고_재시도는_중복차감하지_않는다() {
        queries.today(1L);
        var request = command(answer());
        var result = commands.submit(1L, request);
        assertThat(result.status()).isEqualTo("WON");
        assertThat(result.answer()).isNotNull();
        assertThat(commands.submit(1L, request).attempts()).hasSize(1);
        assertThat(queries.today(1L).statistics().completedGames()).isEqualTo(1);
        assertThatThrownBy(() -> commands.submit(1L, command(answer()))).isInstanceOf(GeneralException.class);
    }

    @Test
    void 오답_진행과_구조화단서를_jsonb에서_복원하고_중복조합은_막는다() {
        queries.today(1L);
        var guess = wrong();
        var request = command(guess);
        commands.submit(1L, request);
        var restored = queries.today(1L);
        assertThat(restored.attempts()).hasSize(1);
        assertThat(restored.statistics().currentStreak()).isZero();
        assertThat(restored.attempts().getFirst().clueText()).isNotBlank();
        assertThat(restored.answer()).isNull();
        assertThat(commands.submit(1L, request).attempts()).hasSize(1);
        assertThatThrownBy(() -> commands.submit(1L, command(guess))).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> commands.submit(1L, new MysteryGuessCommand(day, request.requestId(), answer())))
                .isInstanceOf(GeneralException.class);
        assertThat(queries.today(1L).attempts()).hasSize(1);
    }

    @Test
    void 열번째_오답은_단서없이_종료하고_실패도_스트릭을_유지한다() {
        queries.today(1L);
        var data = cases.findById(day).orElseThrow().getData();
        int count = 0;
        for (var d : data.departments())
            for (var b : data.buildings())
                for (var m : data.methods()) {
                    var guess = new Guess(d.id(), b.id(), m.id());
                    if (guess.equals(data.answer()) || count == 10) continue;
                    commands.submit(1L, command(guess));
                    count++;
                }
        var result = queries.today(1L);
        assertThat(result.status()).isEqualTo("LOST");
        assertThat(result.attempts()).hasSize(10);
        assertThat(result.attempts().getLast().clueText()).isNull();
        assertThat(result.statistics().currentStreak()).isEqualTo(1);
        assertThat(result.statistics().averageAttempts()).isEqualTo(10.0);
        assertThat(result.statistics().successRate()).isZero();
    }

    @Test
    void 동시_재전송은_같은_시도_한개만_기록한다() throws Exception {
        queries.today(1L);
        var request = command(wrong());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var futures = IntStream.range(0, 2)
                    .mapToObj(i -> pool.submit(() -> {
                        start.await();
                        return commands.submit(1L, request);
                    }))
                    .toList();
            start.countDown();
            for (var future : futures)
                assertThat(future.get(15, TimeUnit.SECONDS).attempts()).hasSize(1);
        }
        assertThat(queries.today(1L).attempts()).hasSize(1);
    }

    @Test
    void 동시에_처음_접속해도_하루문제는_하나다() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var a = pool.submit(() -> {
                start.await();
                return queries.today(1L);
            });
            var b = pool.submit(() -> {
                start.await();
                return queries.today(2L);
            });
            start.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS).departments())
                    .isEqualTo(b.get(15, TimeUnit.SECONDS).departments());
        }
        assertThat(cases.count()).isEqualTo(1);
    }

    @Test
    void 이전날을_완료하지_않으면_재접속에서_스트릭과_통계를_초기화한다() {
        queries.today(1L);
        commands.submit(1L, command(answer()));
        given(clock.today()).willReturn(day.plusDays(1));
        queries.today(1L);
        var nextData = cases.findById(day.plusDays(1)).orElseThrow().getData();
        var wrongDepartment = nextData.departments().stream()
                .filter(option -> !option.id().equals(nextData.answer().departmentId()))
                .findFirst()
                .orElseThrow()
                .id();
        var request = new MysteryGuessCommand(
                day.plusDays(1),
                UUID.randomUUID(),
                new Guess(
                        wrongDepartment,
                        nextData.answer().buildingId(),
                        nextData.answer().methodId()));
        var inProgress = commands.submit(1L, request);
        assertThat(inProgress.statistics().currentStreak()).isEqualTo(1);
        assertThat(inProgress.statistics().completedGames()).isEqualTo(1);
        given(clock.today()).willReturn(day.plusDays(2));
        var reset = queries.today(1L);
        assertThat(reset.status()).isEqualTo("NOT_STARTED");
        assertThat(reset.statistics().currentStreak()).isZero();
        assertThat(reset.statistics().completedGames()).isZero();
        assertThat(reset.statistics().wonGames()).isZero();
        assertThat(reset.statistics().successRate()).isZero();
        assertThat(reset.statistics().averageAttempts()).isNull();
        assertThat(reset.statistics().tier()).isEqualTo("BRONZE");
    }

    @Test
    void 자정_이전_화면의_제출은_차감없이_거절한다() {
        queries.today(1L);
        var request = command(wrong());
        given(clock.today()).willReturn(day.plusDays(1));
        assertThatThrownBy(() -> commands.submit(1L, request)).isInstanceOf(GeneralException.class);
        assertThat(queries.today(1L).attempts()).isEmpty();
    }

    @Test
    void 후보에_없는_id는_시작하지_않고_거절한다() {
        assertThatThrownBy(() -> commands.submit(1L, command(new Guess("none", "none", "none"))))
                .isInstanceOf(GeneralException.class);
        assertThat(queries.today(1L).status()).isEqualTo("NOT_STARTED");
    }
}
