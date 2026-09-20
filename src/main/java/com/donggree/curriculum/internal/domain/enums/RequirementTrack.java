package com.donggree.curriculum.internal.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 졸업 요건 세트가 적용되는 과정 구분.
 *
 * <p>대부분의 학과는 과정 구분이 없어 {@link #ALL} 세트 하나만 두면 된다.
 * 공학인증을 운영하는 학과(컴퓨터·AI학부 등)만 일반과정과 심화과정의 졸업 요건이 달라
 * {@link #GENERAL}·{@link #ADVANCED} 세트를 따로 등록한다.
 *
 * <p>학생이 어느 과정인지는 성적표 PDF의 {@code 공학인증심화대상} 값(Y/N)으로 판별한다.
 * 학생 쪽 과정은 언제나 GENERAL 아니면 ADVANCED이며, ALL은 세트에만 쓰는 값이다.
 */
public enum RequirementTrack {

    /** 과정 구분이 없는 학과의 세트. 일반·심화 어느 학생에게나 적용된다. */
    ALL,

    /** 일반과정 세트. 공학인증심화대상이 아닌 학생에게만 적용된다. */
    GENERAL,

    /** 심화과정(공학인증) 세트. 공학인증심화대상 학생에게만 적용된다. */
    ADVANCED;

    /** 성적표의 공학인증심화대상 여부를 학생의 과정 구분으로 변환한다. */
    public static RequirementTrack ofStudent(boolean engineeringCertified) {
        return engineeringCertified ? ADVANCED : GENERAL;
    }

    /**
     * 이 세트가 주어진 과정의 학생에게 적용되는지 판별한다.
     *
     * <p>ALL 세트는 과정 구분이 없는 학과의 것이므로 모든 학생을 받는다.
     * 반면 GENERAL·ADVANCED 세트는 같은 과정의 학생에게만 적용된다 — 심화과정 학생이 일반과정
     * 요건으로 판정되는 일을 막기 위해 교차 매칭을 허용하지 않는다. 그 결과 심화과정 학생인데
     * 심화 세트가 등록돼 있지 않으면 적용할 세트를 찾지 못하고, 미지원 학과와 동일하게 리포트 생성이 실패한다.
     */
    public boolean covers(RequirementTrack studentTrack) {
        return this == ALL || this == studentTrack;
    }

    /**
     * 이 세트와 적용 대상 학생이 겹칠 수 있는 과정 목록. 활성 세트 겹침 검증에 쓴다.
     *
     * <p>GENERAL과 ADVANCED는 서로 다른 학생을 보므로 같은 학과·같은 적용년도에 공존할 수 있다.
     * 반면 ALL은 모든 학생을 받아 어느 과정과도 겹친다.
     */
    public Set<RequirementTrack> conflictingTracks() {
        return this == ALL ? EnumSet.allOf(RequirementTrack.class) : EnumSet.of(ALL, this);
    }
}
