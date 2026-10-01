package com.donggree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

// 실제 컨테이너의 필터 초기화와 Modulith 관측을 함께 검증한다.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:filter-registration-test",
            "spring.jpa.show-sql=false",
            "management.tracing.enabled=true"
        })
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
class DonggreeApplicationTests {

    @Autowired
    private ServletWebServerApplicationContext context;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void Modulith_관측을_활성화한_실제_서버가_기동한다() {
        assertThat(context.getBean("moduleTracingBeanPostProcessor").getClass().getSimpleName())
                .isEqualTo("ModuleObservabilityBeanPostProcessor");
        assertThat(observationRegistry.isNoop()).isFalse();
        assertThat(context.getWebServer().getPort()).isPositive();
    }

    @Test
    void 정상_요청에_요청_ID를_발급한다() {
        var response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
        assertThat(UUID.fromString(response.getHeaders().getFirst("X-Request-ID")))
                .isNotNull();
    }

    @Test
    void 인증_실패도_요청_로그와_운영_지표에_한_번_기록한다(CapturedOutput output) {
        long before = failedSummaryCount();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("INVALID_TEST_TOKEN");

        var response =
                restTemplate.exchange("/api/reports/summary", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("AUTH401_1");
        String requestId = response.getHeaders().getFirst("X-Request-ID");
        assertThat(UUID.fromString(requestId)).isNotNull();

        // 응답 수신 직후에도 컨테이너의 필터 finally 블록은 마무리 중일 수 있다.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(failedSummaryCount()).isEqualTo(before + 1);
            String logs = output.getOut();
            assertThat(logs).contains("request_id=" + requestId).doesNotContain("INVALID_TEST_TOKEN");
            assertThat(logs.lines()
                            .filter(line -> line.contains("event=\"request_failure\""))
                            .toList())
                    .singleElement()
                    .asString()
                    .contains("AUTH401_1", "TOKEN_INVALID");
        });
    }

    private long failedSummaryCount() {
        Timer timer = meterRegistry
                .find("donggree.operation")
                .tags(
                        "operation", "report_summary",
                        "http_status", "401",
                        "outcome", "failure",
                        "actor", "unknown")
                .timer();
        return timer == null ? 0 : timer.count();
    }
}
