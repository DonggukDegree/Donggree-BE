package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.util.regex.Pattern;

/**
 * 승인 필요 요청의 요청자 확인 정보. 관리자가 실존 동아리인지 확인할 때 연락한다.
 *
 * @param contact 이메일 또는 전화번호(100자 이내)
 */
public record ClubRequester(ClubRequesterRole role, String contact) {

    public static final int CONTACT_MAX_LENGTH = 100;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{1,2}-?\\d{3,4}-?\\d{4}$");

    public ClubRequester {
        contact = contact == null ? "" : contact.strip();
        if (role == null) {
            throw new IllegalArgumentException("동아리와의 관계를 골라야 합니다");
        }
        if (contact.length() > CONTACT_MAX_LENGTH
                || !(EMAIL.matcher(contact).matches() || PHONE.matcher(contact).matches())) {
            throw new IllegalArgumentException("확인 연락처는 이메일 또는 전화번호입니다");
        }
    }
}
