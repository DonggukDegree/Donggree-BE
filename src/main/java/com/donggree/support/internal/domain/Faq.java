package com.donggree.support.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.support.internal.domain.enums.FaqTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자주 묻는 질문 한 건. 관리자만 쓰고 사용자는 읽기만 한다.
 * 본문은 마크다운이 아닌 줄바꿈 포함 평문이며, 화면에서 그대로 렌더된다.
 */
@Entity
@Table(name = "faq", indexes = @Index(name = "idx_faq_tag_created_at", columnList = "tag, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseEntity {

    private static final int TITLE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    private FaqTag tag;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    private Faq(FaqTag tag, String title, String content) {
        assignValidated(tag, title, content);
    }

    public static Faq create(FaqTag tag, String title, String content) {
        return new Faq(tag, title, content);
    }

    /** 내용을 전체 교체한다(PUT). 생성과 동일한 불변식을 재검증한다. */
    public void update(FaqTag tag, String title, String content) {
        assignValidated(tag, title, content);
    }

    private void assignValidated(FaqTag tag, String title, String content) {
        if (tag == null) {
            throw new IllegalArgumentException("tag must not be null");
        }
        String validatedTitle = requireText(title, "title");
        if (validatedTitle.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title must be " + TITLE_MAX_LENGTH + " characters or less");
        }
        // 본문은 TEXT 컬럼이라 길이 상한을 두지 않고, 공백만 있는 글만 막는다.
        String validatedContent = requireText(content, "content");

        this.tag = tag;
        this.title = validatedTitle;
        this.content = validatedContent;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
