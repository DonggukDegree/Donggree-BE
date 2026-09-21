package com.donggree.graduation.internal.domain;

/**
 * 한 학과의 졸업 규칙을 학생에게 적용할 때의 전공 역할.
 * 주전공은 복수전공 보유 여부에 따라 나뉘고, SECONDARY는 해당 학과를 복수전공하는 경우다.
 */
public enum MajorRole {
    SINGLE_PRIMARY,
    DUAL_PRIMARY,
    SECONDARY;

    public boolean isPrimary() {
        return this == SINGLE_PRIMARY || this == DUAL_PRIMARY;
    }
}
