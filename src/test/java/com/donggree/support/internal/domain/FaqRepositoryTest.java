package com.donggree.support.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import com.donggree.support.internal.domain.enums.FaqTag;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class FaqRepositoryTest {

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * created_at은 JPA Auditing이 저장 시각으로 채우는데, 연속 저장 시 값이 같아져 정렬 검증이 흔들린다.
     * 저장 후 네이티브 update로 시각을 명시해 순서를 고정한다.
     */
    private Faq save(FaqTag tag, String title, LocalDateTime createdAt) {
        Faq faq = faqRepository.save(Faq.create(tag, title, "본문"));
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE faq SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", faq.getId())
                .executeUpdate();
        return faq;
    }

    @BeforeEach
    void setUp() {
        save(FaqTag.COMMON, "가장 오래된 공통", LocalDateTime.of(2026, 1, 1, 0, 0));
        save(FaqTag.MAJOR, "중간 전공", LocalDateTime.of(2026, 2, 1, 0, 0));
        save(FaqTag.COMMON, "가장 최근 공통", LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.clear();
    }

    @Test
    void 전체를_최신순으로_조회한다() {
        List<Faq> result = faqRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).extracting(Faq::getTitle).containsExactly("가장 최근 공통", "중간 전공", "가장 오래된 공통");
    }

    @Test
    void 태그로_거르면_해당_태그만_최신순으로_조회한다() {
        List<Faq> result = faqRepository.findByTagOrderByCreatedAtDesc(FaqTag.COMMON);

        assertThat(result).extracting(Faq::getTitle).containsExactly("가장 최근 공통", "가장 오래된 공통");
    }

    @Test
    void 해당_태그의_글이_없으면_빈_목록을_반환한다() {
        faqRepository.deleteAll();

        assertThat(faqRepository.findByTagOrderByCreatedAtDesc(FaqTag.MAJOR)).isEmpty();
    }
}
