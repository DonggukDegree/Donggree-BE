package com.donggree.curriculum;

/**
 * 다른 모듈에 졸업 규칙 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * typeName은 graduation 모듈의 RuleEvaluator 분기 식별자로 사용한다 (ex. TOTAL_CREDITS, REQUIRED_COURSE).
 * courseType이 null이면 졸업요건 규칙(총학점, 평점 등)을 의미한다.
 * courseType이 non-null이면 해당 이수 구분 영역에 속하는 규칙이다.
 * ruleConfig는 JSON 문자열이며 typeName별 스키마가 다르다.
 */
public record GraduationRuleView(Long id, String typeName, CourseType courseType, String ruleName, String ruleConfig) {}
