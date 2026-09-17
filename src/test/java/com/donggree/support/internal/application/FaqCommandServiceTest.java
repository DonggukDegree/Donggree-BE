package com.donggree.support.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.support.internal.application.command.FaqCommand;
import com.donggree.support.internal.application.exception.SupportErrorCode;
import com.donggree.support.internal.domain.Faq;
import com.donggree.support.internal.domain.FaqRepository;
import com.donggree.support.internal.domain.enums.FaqTag;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class FaqCommandServiceTest {

    private final FaqRepository faqRepository = Mockito.mock(FaqRepository.class);
    private final FaqCommandService service = new FaqCommandService(faqRepository);

    private Faq faq(Long id) {
        Faq faq = Faq.create(FaqTag.COMMON, "원래 제목", "원래 본문");
        ReflectionTestUtils.setField(faq, "id", id);
        return faq;
    }

    @Test
    void 등록하면_생성된_ID를_반환한다() {
        given(faqRepository.save(any())).willAnswer(invocation -> {
            Faq saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long id = service.create(new FaqCommand(FaqTag.COMMON, "제목", "본문"));

        assertThat(id).isEqualTo(100L);
    }

    @Test
    void 수정하면_태그와_제목과_본문이_교체된다() {
        Faq target = faq(1L);
        given(faqRepository.findById(1L)).willReturn(Optional.of(target));

        service.update(1L, new FaqCommand(FaqTag.MAJOR, "바뀐 제목", "바뀐 본문"));

        assertThat(target.getTag()).isEqualTo(FaqTag.MAJOR);
        assertThat(target.getTitle()).isEqualTo("바뀐 제목");
        assertThat(target.getContent()).isEqualTo("바뀐 본문");
    }

    @Test
    void 없는_FAQ를_수정하면_예외가_발생한다() {
        given(faqRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, new FaqCommand(FaqTag.COMMON, "제목", "본문")))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(SupportErrorCode.FAQ_NOT_FOUND);
    }

    @Test
    void 삭제하면_리포지토리에서_제거한다() {
        Faq target = faq(1L);
        given(faqRepository.findById(1L)).willReturn(Optional.of(target));

        service.delete(1L);

        then(faqRepository).should().delete(target);
    }

    @Test
    void 없는_FAQ를_삭제하면_예외가_발생하고_삭제를_시도하지_않는다() {
        given(faqRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(SupportErrorCode.FAQ_NOT_FOUND);
        then(faqRepository).should(never()).delete(any());
    }
}
