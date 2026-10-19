package com.donggree.user.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** 학교 도메인은 서버에서 결합해 임의 수신지로 메일이 발송되지 않도록 한다. */
public record SchoolEmailAddress(String value) {
    public static final String DOMAIN = "dgu.ac.kr";
    private static final Pattern LOCAL_PART = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9.]{4,28}[a-zA-Z0-9]");

    public SchoolEmailAddress {
        if (value == null || value.indexOf('@') < 0) {
            throw new IllegalArgumentException("지원하지 않는 학교 이메일입니다.");
        }
        String localPart = value.substring(0, value.indexOf('@'));
        String domain = value.substring(value.indexOf('@') + 1);
        if (!DOMAIN.equals(domain)) {
            throw new IllegalArgumentException("지원하지 않는 학교 이메일입니다.");
        }
        if (!LOCAL_PART.matcher(localPart).matches() || localPart.contains("..")) {
            throw new IllegalArgumentException("학교 이메일 아이디를 확인해주세요.");
        }
        value = value.toLowerCase(Locale.ROOT);
    }

    public static SchoolEmailAddress fromLocalPart(String localPart) {
        if (localPart == null || localPart.contains("@")) {
            throw new IllegalArgumentException("학교 이메일 아이디와 도메인을 확인해주세요.");
        }
        return new SchoolEmailAddress(localPart + "@" + DOMAIN);
    }
}
