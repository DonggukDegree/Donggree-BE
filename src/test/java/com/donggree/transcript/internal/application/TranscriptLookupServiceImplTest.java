package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.transcript.internal.domain.Transcript;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TranscriptLookupServiceImplTest {

    private final TranscriptRepository repository = mock(TranscriptRepository.class);
    private final TranscriptLookupServiceImpl service =
            new TranscriptLookupServiceImpl(repository, new TranscriptViewMapper(new ObjectMapper()));

    @Test
    void 저장된_복수1_합격_결과를_주전공과_분리해서_전달한다() {
        Transcript transcript = givenTranscript("{\"meta\":{\"복수1졸업논문심사\":\"합격\"}}");
        given(transcript.isThesisStatus()).willReturn(false);

        var view = service.findByMemberId(1L).orElseThrow();

        assertThat(view.thesisStatus()).isFalse();
        assertThat(view.dualMajor1ThesisStatus()).isTrue();
        assertThat(service.findById(1L).orElseThrow().dualMajor1ThesisStatus()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"meta\":{}}",
                "{\"meta\":{\"복수1졸업논문심사\":null}}",
                "{\"meta\":{\"복수1졸업논문심사\":\"미판정\"}}",
                "{\"meta\":{\"복수1졸업논문심사\":\"불합격\"}}",
                "{\"meta\":{\"졸업논문심사\":\"합격\",\"복수2졸업논문심사\":\"합격\"}}",
                "invalid-json"
            })
    void 복수1의_명시적_합격이_없으면_주전공_합격으로_대체하지_않는다(String rawData) {
        Transcript transcript = givenTranscript(rawData);
        given(transcript.isThesisStatus()).willReturn(true);

        var view = service.findByMemberId(1L).orElseThrow();

        assertThat(view.thesisStatus()).isTrue();
        assertThat(view.dualMajor1ThesisStatus()).isFalse();
    }

    private Transcript givenTranscript(String rawData) {
        Transcript transcript = mock(Transcript.class);
        given(transcript.getDualMajor1Id()).willReturn(200L);
        given(transcript.getRawData()).willReturn(rawData);
        given(repository.findWithCourseRecordsByMemberId(1L)).willReturn(Optional.of(transcript));
        given(repository.findWithCourseRecordsById(1L)).willReturn(Optional.of(transcript));
        return transcript;
    }
}
