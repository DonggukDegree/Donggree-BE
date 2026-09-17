package com.donggree.support.internal.application;

import com.donggree.support.internal.application.projection.FaqProjection;
import com.donggree.support.internal.domain.Faq;
import com.donggree.support.internal.domain.FaqRepository;
import com.donggree.support.internal.domain.enums.FaqTag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FAQ 조회(Query) 응용 서비스. 사용자 화면과 관리자 화면이 같은 목록을 공유한다.
 */
@Service
@RequiredArgsConstructor
public class FaqQueryService {

    private final FaqRepository faqRepository;

    /**
     * FAQ 목록을 최신순으로 조회한다. tag가 주어지면 해당 태그만, 없으면 전체를 반환한다.
     * (화면 상단 칩의 '전체'가 tag 미지정에 해당한다.)
     */
    @Transactional(readOnly = true)
    public List<FaqProjection> getFaqs(FaqTag tag) {
        List<Faq> faqs = (tag == null)
                ? faqRepository.findAllByOrderByCreatedAtDesc()
                : faqRepository.findByTagOrderByCreatedAtDesc(tag);

        return faqs.stream()
                .map(f -> new FaqProjection(f.getId(), f.getTag(), f.getTitle(), f.getContent()))
                .toList();
    }
}
