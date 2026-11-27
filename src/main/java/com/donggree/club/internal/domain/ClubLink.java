package com.donggree.club.internal.domain;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 관련 링크. 화면에 보일 이름(1~10자)과 https 주소만 받는다.
 *
 * @param label 표시 이름(예: 인스타그램)
 * @param url   https 주소
 */
public record ClubLink(String label, String url) {

    public static final int LABEL_MAX_LENGTH = 10;
    public static final int URL_MAX_LENGTH = 500;
    public static final int MAX_COUNT = 3;

    public ClubLink {
        label = label == null ? "" : label.strip();
        url = url == null ? "" : url.strip();
        if (label.isEmpty() || label.length() > LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("링크 이름은 1~10자입니다");
        }
        if (url.length() > URL_MAX_LENGTH || !isHttps(url)) {
            throw new IllegalArgumentException("링크는 https 주소만 받습니다");
        }
    }

    private static boolean isHttps(String value) {
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
