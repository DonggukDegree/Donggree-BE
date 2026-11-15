package com.donggree.point.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.place.event.PlaceInfoEditedEvent;
import com.donggree.place.event.PlaceRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 장소 활동 적립이 원래 요청의 응답 전(커밋 직후, 같은 스레드)에 끝나야 화면이 바로 적립을 안내할 수 있다. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.jpa.show-sql=false"})
class PointRewardTimingTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PointQueryService queryService;

    @Test
    void 장소_활동_적립은_원래_트랜잭션이_커밋된_직후_같은_요청_안에서_끝난다() {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> eventPublisher.publishEvent(new PlaceInfoEditedEvent(10L, 900L)));

        assertThat(queryService.getTotalPoints(900L)).isEqualTo(100);
    }

    @Test
    void 원래_트랜잭션이_롤백되면_적립하지_않는다() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PlaceRegisteredEvent(11L, 901L));
            status.setRollbackOnly();
        });

        assertThat(queryService.getTotalPoints(901L)).isZero();
    }
}
