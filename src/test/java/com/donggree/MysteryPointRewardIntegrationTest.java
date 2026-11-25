package com.donggree;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.donggree.mystery.internal.application.*;
import com.donggree.mystery.internal.application.command.MysteryGuessCommand;
import com.donggree.mystery.internal.domain.*;
import com.donggree.mystery.internal.domain.MysteryData.Guess;
import com.donggree.mystery.internal.infrastructure.MysteryClock;
import com.donggree.point.internal.application.PointQueryService;
import com.donggree.point.internal.application.PointRewardService;
import com.donggree.point.internal.domain.PointHistoryRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 정답 처리와 포인트 모듈 사이의 커밋·롤백·동시 재시도 계약 검증. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.jpa.show-sql=false")
class MysteryPointRewardIntegrationTest {
    @Autowired
    MysteryCommandService commands;

    @Autowired
    MysteryQueryService queries;

    @Autowired
    DailyMysteryCaseRepository cases;

    @Autowired
    MysteryPlayerRepository players;

    @Autowired
    PointQueryService points;

    @Autowired
    PointHistoryRepository histories;

    @Autowired
    PlatformTransactionManager transactions;

    @MockitoBean
    MysteryClock clock;

    @MockitoSpyBean
    PointRewardService rewards;

    final LocalDate day = LocalDate.of(2026, 11, 24);

    @BeforeEach
    void setup() {
        given(clock.today()).willReturn(day);
    }

    @AfterEach
    void clean() {
        histories.deleteAll();
        players.deleteAll();
        cases.deleteAll();
    }

    MysteryGuessCommand winningRequest() {
        return new MysteryGuessCommand(
                clock.today(),
                UUID.randomUUID(),
                cases.findById(clock.today()).orElseThrow().getData().answer());
    }

    @Test
    void 정답과_10P를_함께_저장하고_재전송과_재조회에는_중복_지급하지_않는다() {
        queries.today(1L);
        var request = winningRequest();
        assertThat(commands.submit(1L, request).status()).isEqualTo("WON");
        commands.submit(1L, request);
        queries.today(1L);
        assertThat(points.getTotalPoints(1L)).isEqualTo(10);
        given(clock.today()).willReturn(day.plusDays(1));
        queries.today(1L);
        commands.submit(1L, winningRequest());
        assertThat(points.getTotalPoints(1L)).isEqualTo(20);
    }

    @Test
    void 열번_오답으로_실패해도_포인트는_지급하지_않는다() {
        queries.today(1L);
        var data = cases.findById(day).orElseThrow().getData();
        var guesses = data.departments().stream()
                .flatMap(d -> data.buildings().stream()
                        .flatMap(b -> data.methods().stream().map(m -> new Guess(d.id(), b.id(), m.id()))))
                .filter(g -> !g.equals(data.answer()))
                .limit(10)
                .toList();
        for (var guess : guesses) commands.submit(1L, new MysteryGuessCommand(day, UUID.randomUUID(), guess));
        assertThat(queries.today(1L).status()).isEqualTo("LOST");
        assertThat(points.getTotalPoints(1L)).isZero();
    }

    @Test
    void 열번째_시도에_맞혀도_10P를_지급한다() {
        queries.today(1L);
        var data = cases.findById(day).orElseThrow().getData();
        var guesses = data.departments().stream()
                .flatMap(d -> data.buildings().stream()
                        .flatMap(b -> data.methods().stream().map(m -> new Guess(d.id(), b.id(), m.id()))))
                .filter(g -> !g.equals(data.answer()))
                .limit(9)
                .toList();
        for (var guess : guesses) commands.submit(1L, new MysteryGuessCommand(day, UUID.randomUUID(), guess));
        var result = commands.submit(1L, winningRequest());
        assertThat(result.status()).isEqualTo("WON");
        assertThat(result.attempts()).hasSize(10);
        assertThat(points.getTotalPoints(1L)).isEqualTo(10);
    }

    @Test
    void 동시_정답_재전송에도_10P를_한번만_지급한다() throws Exception {
        queries.today(1L);
        var request = winningRequest();
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
                assertThat(future.get(10, TimeUnit.SECONDS).status()).isEqualTo("WON");
        }
        assertThat(points.getTotalPoints(1L)).isEqualTo(10);
    }

    @Test
    void 정답_트랜잭션을_롤백하면_보상도_남지_않는다() {
        queries.today(1L);
        var request = winningRequest();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            commands.submit(1L, request);
            status.setRollbackOnly();
        });
        assertThat(queries.today(1L).attempts()).isEmpty();
        assertThat(points.getTotalPoints(1L)).isZero();
    }

    @Test
    void 적립에_실패하면_정답도_롤백하고_같은_요청으로_재시도할_수_있다() {
        queries.today(1L);
        var request = winningRequest();
        doThrow(new IllegalStateException("test reward failure"))
                .doCallRealMethod()
                .when(rewards)
                .rewardMysterySolved(1L, day);
        assertThatThrownBy(() -> commands.submit(1L, request)).isInstanceOf(IllegalStateException.class);
        assertThat(queries.today(1L).attempts()).isEmpty();
        assertThat(points.getTotalPoints(1L)).isZero();
        assertThat(commands.submit(1L, request).status()).isEqualTo("WON");
        assertThat(points.getTotalPoints(1L)).isEqualTo(10);
    }
}
