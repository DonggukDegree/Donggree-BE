package com.donggree.support.internal.domain;

import com.donggree.support.internal.domain.enums.FaqTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByCreatedAtDesc();

    List<Faq> findByTagOrderByCreatedAtDesc(FaqTag tag);
}
