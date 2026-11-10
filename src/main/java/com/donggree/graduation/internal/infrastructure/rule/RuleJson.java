package com.donggree.graduation.internal.infrastructure.rule;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 기존 규칙 해석의 기본 설정을 유지하며 HTTP ObjectMapper 설정과 분리한다. */
final class RuleJson {
    static final ObjectMapper MAPPER = new ObjectMapper();

    private RuleJson() {}
}
