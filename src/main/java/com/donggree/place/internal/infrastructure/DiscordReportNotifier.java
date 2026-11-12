package com.donggree.place.internal.infrastructure;

import com.donggree.place.internal.application.ReportNotifier;
import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.application.event.ReviewReportedEvent;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 장소·리뷰 신고 접수를 디스코드 웹훅으로 알린다. 신고자·작성자 정보는 보내지 않는다.
 *
 * <p>사용자가 쓴 신고 내용에 @everyone 같은 멘션이 있어도 울리지 않도록 allowed_mentions를 비운다.
 * 알림 실패는 신고 접수와 무관하므로 경고 로그만 남기고 다시 던지지 않는다(웹훅 주소는 로그에 남기지 않음).
 */
@Slf4j
@Component
public class DiscordReportNotifier implements ReportNotifier {

    static final int CONTENT_MAX_LENGTH = 2_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final Map<PlaceReportReason, String> REASON_LABELS = Map.of(
            PlaceReportReason.CLOSED, "폐업",
            PlaceReportReason.WRONG_LOCATION, "위치 오류",
            PlaceReportReason.WRONG_NAME, "이름 오류",
            PlaceReportReason.DUPLICATE, "중복 장소",
            PlaceReportReason.INAPPROPRIATE, "부적절한 내용",
            PlaceReportReason.OTHER, "기타");
    private static final Map<ReviewReportReason, String> REVIEW_REASON_LABELS = Map.of(
            ReviewReportReason.ABUSE, "욕설·비방",
            ReviewReportReason.SPAM, "광고·도배",
            ReviewReportReason.FALSE_INFO, "허위 내용",
            ReviewReportReason.PRIVACY, "개인정보 노출",
            ReviewReportReason.OTHER, "기타");

    private final ReportNotificationProperties properties;
    private final RestClient restClient;

    @Autowired
    public DiscordReportNotifier(ReportNotificationProperties properties, RestClient.Builder restClientBuilder) {
        this(
                properties,
                restClientBuilder.requestFactory(timeoutRequestFactory()).build());
    }

    /** 테스트에서 가짜 서버에 묶은 클라이언트를 넣는다. */
    DiscordReportNotifier(ReportNotificationProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return requestFactory;
    }

    @Override
    public void notifyPlaceReported(PlaceReportedEvent event) {
        send("place", event.reportId(), content(event));
    }

    @Override
    public void notifyReviewReported(ReviewReportedEvent event) {
        send("review", event.reportId(), content(event));
    }

    private void send(String target, Long reportId, String content) {
        if (!properties.enabled()) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri(properties.discordWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content, "allowed_mentions", Map.of("parse", List.of())))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn(
                    "신고 알림 전송 실패 event=\"report_notification_failure\" target=\"{}\" report_id=\"{}\""
                            + " exception_type=\"{}\"",
                    target,
                    reportId,
                    exception.getClass().getSimpleName());
        }
    }

    static String content(ReviewReportedEvent event) {
        StringBuilder content = new StringBuilder()
                .append("🚨 동글맵 리뷰 신고 #")
                .append(event.reportId())
                .append("\n장소: ")
                .append(event.placeName())
                .append(" (ID ")
                .append(event.placeId())
                .append(")\n리뷰: ID ")
                .append(event.reviewId())
                .append(" · ★")
                .append(event.rating())
                .append(" · ")
                .append(event.reviewExcerpt())
                .append("\n사유: ")
                .append(REVIEW_REASON_LABELS.get(event.reason()));
        if (event.description() != null) {
            content.append("\n내용: ").append(event.description());
        }
        content.append("\n처리 대기 신고 ").append(event.pendingCount()).append("건");
        if (event.autoHidden()) {
            content.append(" · 리뷰 자동 숨김됨");
        }
        return limit(content);
    }

    static String content(PlaceReportedEvent event) {
        StringBuilder content = new StringBuilder()
                .append("🚨 동글맵 장소 신고 #")
                .append(event.reportId())
                .append("\n장소: ")
                .append(event.placeName())
                .append(" (ID ")
                .append(event.placeId())
                .append(")\n사유: ")
                .append(REASON_LABELS.get(event.reason()));
        if (event.description() != null) {
            content.append("\n내용: ").append(event.description());
        }
        content.append("\n처리 대기 신고 ").append(event.pendingCount()).append("건");
        if (event.autoHidden()) {
            content.append(" · 자동 숨김됨");
        }
        return limit(content);
    }

    private static String limit(StringBuilder content) {
        return content.length() > CONTENT_MAX_LENGTH
                ? content.substring(0, CONTENT_MAX_LENGTH - 1) + "…"
                : content.toString();
    }
}
