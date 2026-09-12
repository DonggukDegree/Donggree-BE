package com.donggree.graduation.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluationContextTest {

    private final EvaluationContext context = new EvaluationContext(
            new TranscriptView(
                    1L,
                    1L,
                    100L,
                    null,
                    null,
                    null,
                    null,
                    2023,
                    "단일",
                    0,
                    BigDecimal.valueOf(4.0),
                    "S1",
                    false,
                    null,
                    null,
                    false,
                    List.of()),
            Map.of());

    @Test
    void codeMatchesAny_정확히_일치하면_true() {
        assertThat(context.codeMatchesAny("CSE3001", List.of("CSE3001"))).isTrue();
    }

    @Test
    void codeMatchesAny_prefix_패턴과_일치하면_true() {
        assertThat(context.codeMatchesAny("DAI3001", List.of("DAI*"))).isTrue();
    }

    @Test
    void codeMatchesAny_일치하는_패턴이_없으면_false() {
        assertThat(context.codeMatchesAny("CSE3001", List.of("CSE9999", "DAI*")))
                .isFalse();
    }

    @Test
    void codeMatchesAny_학수번호가_null이면_false() {
        assertThat(context.codeMatchesAny(null, List.of("CSE3001"))).isFalse();
    }

    @Test
    void codeMatchesAny_패턴이_null이면_false() {
        assertThat(context.codeMatchesAny("CSE3001", null)).isFalse();
    }
}
