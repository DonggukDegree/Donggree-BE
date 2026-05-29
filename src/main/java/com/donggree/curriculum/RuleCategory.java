package com.donggree.curriculum;

/**
 * 졸업 규칙이 속하는 부문 enum.
 * GRADUATION_REQ 부문의 미충족 규칙만 리포트 요약의 "미충족사유"에 노출된다.
 * graduation 모듈에서도 참조하므로 public 패키지에 둔다.
 */
public enum RuleCategory {
    LIBERAL,
    MAJOR,
    GRADUATION_REQ
}
