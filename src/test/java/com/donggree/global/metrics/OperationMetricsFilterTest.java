package com.donggree.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OperationMetricsFilterTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OperationMetricsFilter filter = new OperationMetricsFilter(registry);
    private final OperationMetricsAdvice advice = new OperationMetricsAdvice();

    @Test
    void 저장_성공의_실제_HTTP_상태와_업무_코드를_구분한다() throws Exception {
        var request = new MockHttpServletRequest("PUT", "/api/users/me/reports");
        var response = new MockHttpServletResponse();
        request.setAttribute(OperationMetricsFilter.ACTOR, "student");
        filter.doFilter(
                request,
                response,
                (req, res) -> advice.beforeBodyWrite(
                        ApiResponse.onSuccess(GeneralSuccessCode.CREATED, "민감한 원문"),
                        null,
                        MediaType.APPLICATION_JSON,
                        MappingJackson2HttpMessageConverter.class,
                        new ServletServerHttpRequest(request),
                        new ServletServerHttpResponse(response)));
        var meter = registry.get("donggree.operation")
                .tags(
                        "operation",
                        "pdf_upload",
                        "outcome",
                        "success",
                        "code",
                        "COMMON201_1",
                        "http_status",
                        "200",
                        "actor",
                        "student")
                .timer();
        assertThat(meter.count()).isEqualTo(1);
        assertThat(meter.getId().getTags().toString()).doesNotContain("민감한", "memberId", "file");
    }

    @Test
    void 업무_실패와_관리자_요청을_분류한다() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/reports/summary");
        var response = new MockHttpServletResponse();
        request.setAttribute(OperationMetricsFilter.ACTOR, "admin");
        filter.doFilter(request, response, (req, res) -> {
            response.setStatus(400);
            advice.beforeBodyWrite(
                    ApiResponse.onFailure(GeneralErrorCode.BAD_REQUEST),
                    null,
                    MediaType.APPLICATION_JSON,
                    MappingJackson2HttpMessageConverter.class,
                    new ServletServerHttpRequest(request),
                    new ServletServerHttpResponse(response));
        });
        assertThat(registry.get("donggree.operation")
                        .tags("outcome", "failure", "actor", "admin", "http_status", "400", "code", "COMMON400_1")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void 저장_완료_후_트랜잭션_예외는_성공으로_세지_않는다() {
        var request = new MockHttpServletRequest("PUT", "/api/users/me/reports");
        request.setAttribute(OperationMetricsFilter.CODE, "COMMON201_1");
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    throw new ServletException("회원번호나 원문을 포함할 수 있는 예외");
                }))
                .isInstanceOf(ServletException.class);
        assertThat(registry.get("donggree.operation")
                        .tags("outcome", "failure", "code", "UNHANDLED_ERROR")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void 보안_필터의_인증_실패도_측정한다() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/reports/summary");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> response.setStatus(401));
        assertThat(registry.get("donggree.operation")
                        .tags("code", "HTTP_ERROR", "http_status", "401", "outcome", "failure", "actor", "unknown")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"success", "failure"})
    void OAuth_리다이렉트_302와_로그인_성공실패를_구분한다(String outcome) throws Exception {
        var request = new MockHttpServletRequest("GET", "/oauth/callback/kakao");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            response.setStatus(302);
            request.setAttribute(OperationMetricsFilter.OUTCOME, outcome);
            request.setAttribute(
                    OperationMetricsFilter.CODE, outcome.equals("success") ? "LOGIN_SUCCESS" : "LOGIN_FAILURE");
        });
        assertThat(registry.get("donggree.operation")
                        .tags("operation", "login", "http_status", "302", "outcome", outcome)
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void 재요청은_별도_서버_요청으로_계수하고_관리자_미리보기는_제외한다() throws Exception {
        for (int i = 0; i < 2; i++)
            filter.doFilter(
                    new MockHttpServletRequest("GET", "/api/reports/summary"),
                    new MockHttpServletResponse(),
                    (req, res) -> {});
        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/admin/reports/preview"),
                new MockHttpServletResponse(),
                (req, res) -> {});
        assertThat(registry.getMeters()).hasSize(1);
        assertThat(registry.get("donggree.operation").timer().count()).isEqualTo(2);
    }

    @Test
    void 측정_장애는_업무_응답에_영향을_주지_않는다() throws Exception {
        MeterRegistry broken = mock(MeterRegistry.class);
        when(broken.config()).thenThrow(new IllegalStateException("측정 장애"));
        var executed = new AtomicBoolean();
        new OperationMetricsFilter(broken)
                .doFilter(
                        new MockHttpServletRequest("GET", "/api/reports/summary"),
                        new MockHttpServletResponse(),
                        (req, res) -> executed.set(true));
        assertThat(executed).isTrue();
    }
}
