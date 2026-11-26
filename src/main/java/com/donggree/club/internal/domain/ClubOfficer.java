package com.donggree.club.internal.domain;

/**
 * 운영진 한 명. 직책·이름은 필수, 학과는 비울 수 있다(빈 문자열).
 *
 * @param role       직책(예: 회장)
 * @param department 학과
 * @param name       이름
 */
public record ClubOfficer(String role, String department, String name) {

    public static final int TEXT_MAX_LENGTH = 20;
    public static final int MAX_COUNT = 5;

    public ClubOfficer {
        role = role == null ? "" : role.strip();
        department = department == null ? "" : department.strip();
        name = name == null ? "" : name.strip();
        if (role.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("운영진은 직책과 이름을 함께 입력합니다");
        }
        if (role.length() > TEXT_MAX_LENGTH
                || department.length() > TEXT_MAX_LENGTH
                || name.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException("운영진 항목은 20자 이내입니다");
        }
    }
}
