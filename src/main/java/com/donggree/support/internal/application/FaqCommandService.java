package com.donggree.support.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.support.internal.application.command.FaqCommand;
import com.donggree.support.internal.application.exception.SupportErrorCode;
import com.donggree.support.internal.domain.Faq;
import com.donggree.support.internal.domain.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FAQ 변경(Command) 응용 서비스. 관리자 화면의 등록·수정·삭제를 담당한다.
 * 트랜잭션 경계만 잡고 값 검증은 도메인(Faq)에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class FaqCommandService {

    private final FaqRepository faqRepository;

    @Transactional
    public Long create(FaqCommand command) {
        Faq saved = faqRepository.save(Faq.create(command.tag(), command.title(), command.content()));
        return saved.getId();
    }

    /** FAQ를 전체 교체한다(PUT). 태그·제목·본문을 모두 요청값으로 덮어쓴다. */
    @Transactional
    public void update(Long id, FaqCommand command) {
        Faq faq = faqRepository.findById(id).orElseThrow(() -> new GeneralException(SupportErrorCode.FAQ_NOT_FOUND));
        faq.update(command.tag(), command.title(), command.content());
    }

    @Transactional
    public void delete(Long id) {
        Faq faq = faqRepository.findById(id).orElseThrow(() -> new GeneralException(SupportErrorCode.FAQ_NOT_FOUND));
        faqRepository.delete(faq);
    }
}
