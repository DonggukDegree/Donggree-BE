package com.donggree.place.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordReportNotifierTest {

    private static final String WEBHOOK = "https://discord.example.invalid/api/webhooks/test";

    private final PlaceReportedEvent event =
            new PlaceReportedEvent(12L, 3L, "동국반점", PlaceReportReason.OTHER, "@everyone 주차장 입구예요", 3L, true);

    @Test
    void 신고자_정보_없이_장소와_사유와_내용과_대기_수를_보내고_멘션은_막는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(WEBHOOK))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.allowed_mentions.parse", Matchers.empty()))
                .andExpect(jsonPath("$.content", Matchers.containsString("🚨 동글맵 장소 신고 #12")))
                .andExpect(jsonPath("$.content", Matchers.containsString("장소: 동국반점 (ID 3)")))
                .andExpect(jsonPath("$.content", Matchers.containsString("사유: 기타")))
                .andExpect(jsonPath("$.content", Matchers.containsString("처리 대기 신고 3건 · 자동 숨김됨")))
                .andRespond(withSuccess());

        new DiscordReportNotifier(new ReportNotificationProperties(WEBHOOK), builder.build())
                .notifyPlaceReported(event);

        server.verify();
    }

    @Test
    void 웹훅_주소가_없으면_보내지_않는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        new DiscordReportNotifier(new ReportNotificationProperties(" "), builder.build()).notifyPlaceReported(event);

        server.verify();
    }

    @Test
    void 전송_실패는_신고_접수에_영향을_주지_않도록_삼킨다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(WEBHOOK)).andRespond(withServerError());

        assertThatCode(() -> new DiscordReportNotifier(new ReportNotificationProperties(WEBHOOK), builder.build())
                        .notifyPlaceReported(event))
                .doesNotThrowAnyException();
    }

    @Test
    void 설명이_없으면_내용_줄을_빼고_2000자를_넘지_않는다() {
        String content = DiscordReportNotifier.content(
                new PlaceReportedEvent(1L, 3L, "가".repeat(3000), PlaceReportReason.CLOSED, null, 1L, false));

        assertThat(content).doesNotContain("내용:").hasSize(DiscordReportNotifier.CONTENT_MAX_LENGTH);
        assertThat(DiscordReportNotifier.content(
                        new PlaceReportedEvent(1L, 3L, "동국반점", PlaceReportReason.CLOSED, null, 1L, false)))
                .endsWith("처리 대기 신고 1건");
    }
}
