package com.donggree.club.internal.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 관리자가 입력하는 배너 내용과 노출 조건.
 *
 * <p>글자 배너는 배경과 라벨(12자)·제목(30자)·설명(60자)을, 이미지 배너는 PC 이미지(필수)·모바일 이미지와 대체 텍스트(제목)를 쓴다.
 * 링크와 이미지 주소는 동그리 안 경로('/'로 시작, '//' 제외) 또는 https 주소만 받는다.
 *
 * @param background 글자 배너의 CSS 배경(색·그라데이션). 영문·숫자와 # ( ) , . % - 공백만 받는다
 * @param startsAt   노출 시작(포함). null이면 바로
 * @param endsAt     노출 종료(제외). null이면 계속
 */
public record BannerContent(
        String title,
        String eyebrow,
        String description,
        String background,
        String imageUrl,
        String mobileImageUrl,
        String link,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active) {

    public static final int TITLE_MAX_LENGTH = 30;
    public static final int EYEBROW_MAX_LENGTH = 12;
    public static final int DESCRIPTION_MAX_LENGTH = 60;
    public static final int BACKGROUND_MAX_LENGTH = 200;
    public static final int URL_MAX_LENGTH = 500;
    private static final Pattern BACKGROUND = Pattern.compile("^[A-Za-z0-9#(),.%\\s-]+$");

    public BannerContent {
        title = trim(title);
        eyebrow = blankToNull(eyebrow);
        description = blankToNull(description);
        background = blankToNull(background);
        imageUrl = blankToNull(imageUrl);
        mobileImageUrl = blankToNull(mobileImageUrl);
        link = trim(link);
        if (title.isEmpty() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("배너 제목은 1~30자입니다");
        }
        if (!isAllowedUrl(link)) {
            throw new IllegalArgumentException("링크는 동그리 경로 또는 https 주소입니다");
        }
        if (imageUrl == null) {
            if (mobileImageUrl != null) throw new IllegalArgumentException("모바일 이미지는 PC 이미지와 함께 씁니다");
            if (background == null
                    || background.length() > BACKGROUND_MAX_LENGTH
                    || !BACKGROUND.matcher(background).matches()) {
                throw new IllegalArgumentException("글자 배너는 배경을 골라야 합니다");
            }
            if (eyebrow != null && eyebrow.length() > EYEBROW_MAX_LENGTH) {
                throw new IllegalArgumentException("라벨은 12자 이내입니다");
            }
            if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
                throw new IllegalArgumentException("설명은 60자 이내입니다");
            }
        } else {
            if (!isAllowedUrl(imageUrl) || (mobileImageUrl != null && !isAllowedUrl(mobileImageUrl))) {
                throw new IllegalArgumentException("이미지는 동그리 경로 또는 https 주소입니다");
            }
            eyebrow = null;
            description = null;
            background = null;
        }
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("노출 종료는 시작 이후여야 합니다");
        }
    }

    /** 켜져 있고 노출 기간(시작 포함·종료 제외) 안이면 공개한다. */
    public boolean visibleAt(LocalDateTime now) {
        return active && (startsAt == null || !now.isBefore(startsAt)) && (endsAt == null || now.isBefore(endsAt));
    }

    static boolean isAllowedUrl(String value) {
        if (value == null || value.isEmpty() || value.length() > URL_MAX_LENGTH) return false;
        if (value.startsWith("/")) return !value.startsWith("//") && !value.contains("\\");
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
