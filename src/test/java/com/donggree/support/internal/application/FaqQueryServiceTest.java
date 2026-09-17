package com.donggree.support.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.donggree.support.internal.application.projection.FaqProjection;
import com.donggree.support.internal.domain.Faq;
import com.donggree.support.internal.domain.FaqRepository;
import com.donggree.support.internal.domain.enums.FaqTag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class FaqQueryServiceTest {

    private final FaqRepository faqRepository = Mockito.mock(FaqRepository.class);
    private final FaqQueryService service = new FaqQueryService(faqRepository);

    private Faq faq(Long id, FaqTag tag, String title, String content) {
        Faq faq = Faq.create(tag, title, content);
        ReflectionTestUtils.setField(faq, "id", id);
        return faq;
    }

    @Test
    void 태그를_지정하지_않으면_전체를_최신순으로_조회한다() {
        given(faqRepository.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(faq(2L, FaqTag.MAJOR, "전공 질문", "전공 답변"), faq(1L, FaqTag.COMMON, "공통 질문", "공통 답변")));

        List<FaqProjection> result = service.getFaqs(null);

        assertThat(result).extracting(FaqProjection::id).containsExactly(2L, 1L);
        assertThat(result.get(0).tag()).isEqualTo(FaqTag.MAJOR);
        then(faqRepository).should(never()).findByTagOrderByCreatedAtDesc(Mockito.any());
    }

    @Test
    void 태그를_지정하면_해당_태그만_조회한다() {
        given(faqRepository.findByTagOrderByCreatedAtDesc(FaqTag.MAJOR))
                .willReturn(List.of(faq(2L, FaqTag.MAJOR, "전공 질문", "전공 답변")));

        List<FaqProjection> result = service.getFaqs(FaqTag.MAJOR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("전공 질문");
        then(faqRepository).should(never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void 서비스_태그도_동일하게_걸러_조회한다() {
        given(faqRepository.findByTagOrderByCreatedAtDesc(FaqTag.SERVICE))
                .willReturn(List.of(faq(3L, FaqTag.SERVICE, "성적표는 어디서 받나요?", "nDRIMS에서 받을 수 있어요.")));

        List<FaqProjection> result = service.getFaqs(FaqTag.SERVICE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tag()).isEqualTo(FaqTag.SERVICE);
    }

    @Test
    void 글이_없으면_빈_목록을_반환한다() {
        given(faqRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        assertThat(service.getFaqs(null)).isEmpty();
    }
}
