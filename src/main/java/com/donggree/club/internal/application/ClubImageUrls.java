package com.donggree.club.internal.application;

import com.donggree.club.internal.domain.ClubImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 사진 주소. 공개 주소(club.image.public-url) 뒤에 저장 이름을 붙인다.
 *
 * <p>디스크 저장은 API 서버 경로(/api/clubs/images/), R2 저장은 R2에 연결한 공개 도메인(https)을 쓴다.
 */
@Component
public class ClubImageUrls {

    public static final String API_PATH = "/api/clubs/images/";

    private final String base;

    public ClubImageUrls(@Value("${club.image.public-url:" + API_PATH + "}") String base) {
        String value = base == null || base.isBlank() ? API_PATH : base.strip();
        this.base = value.endsWith("/") ? value : value + "/";
    }

    public String of(ClubImage image) {
        return base + image.getStoredName();
    }
}
