package com.donggree.club.internal.domain;

import java.util.Optional;

/** 받는 사진 형식. 파일 이름·요청 헤더가 아니라 파일 앞부분(시그니처)으로 판단한다. */
public enum ImageFormat {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private final String contentType;
    private final String extension;

    ImageFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageFormat> detect(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return Optional.empty();
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return Optional.of(PNG);
        }
        if (bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<ImageFormat> fromContentType(String contentType) {
        for (ImageFormat format : values()) {
            if (format.contentType.equals(contentType)) return Optional.of(format);
        }
        return Optional.empty();
    }
}
