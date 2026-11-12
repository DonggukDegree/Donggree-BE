package com.donggree.place.internal.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 신고 접수 운영 알림 설정. 웹훅 주소는 비밀값이라 환경변수로만 받고 로그·문서에 남기지 않는다.
 *
 * @param discordWebhookUrl 디스코드 채널 웹훅 주소. 비우면 알림을 보내지 않는다
 */
@ConfigurationProperties(prefix = "app.report-notification")
public record ReportNotificationProperties(String discordWebhookUrl) {

    public boolean enabled() {
        return discordWebhookUrl != null && !discordWebhookUrl.isBlank();
    }
}
