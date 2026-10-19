package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.transcript.internal.application.pdf.TranscriptPdfReader;
import com.donggree.transcript.internal.domain.transcript.TranscriptCreateData;
import com.donggree.transcript.internal.domain.transcript.TranscriptRepository;
import com.donggree.user.internal.application.UserIdentityServiceImpl;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 서비스 프록시·H2로 성적표 저장과 회원 본인 인증의 동시 커밋·롤백 검증. */
@DataJpaTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:transcript-identity-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TranscriptCommandService.class, UserIdentityServiceImpl.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TranscriptIdentityTransactionIntegrationTest {
    @Autowired
    private TranscriptCommandService commandService;

    @Autowired
    private TranscriptRepository transcriptRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TranscriptPdfReader pdfReader;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = Member.registerKakaoMember("identity-test", "identity@example.com");
        member.completeOnboarding("2023000001", "테스트");
        memberId = memberRepository.saveAndFlush(member).getId();
    }

    @AfterEach
    void cleanUp() {
        transcriptRepository.deleteAll();
        memberRepository.deleteById(memberId);
    }

    @Test
    void 업로드_서비스가_반환되면_본인_인증과_성적표가_함께_확정된다() {
        upload();
        assertThat(memberRepository.findById(memberId).orElseThrow().isIdentityVerified())
                .isTrue();
        assertThat(transcriptRepository.findByMemberId(memberId)).isPresent();
    }

    @Test
    void 인증_후_트랜잭션이_실패하면_회원_인증과_성적표가_함께_롤백된다() {
        var transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    upload();
                    assertThat(memberRepository.findById(memberId).orElseThrow().isIdentityVerified())
                            .isTrue();
                    assertThat(transcriptRepository.findByMemberId(memberId)).isPresent();
                    throw new IllegalStateException("저장 후 실패");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("저장 후 실패");
        assertThat(memberRepository.findById(memberId).orElseThrow().isIdentityVerified())
                .isFalse();
        assertThat(transcriptRepository.findByMemberId(memberId)).isEmpty();
    }

    private void upload() {
        var data = new TranscriptCreateData(
                memberId,
                "{}",
                2023,
                "재학",
                "학사과정",
                null,
                null,
                null,
                null,
                null,
                0,
                BigDecimal.ZERO,
                1,
                "S1",
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                false);
        commandService.createTranscript(data, List.of(), "2023000001", "테스트");
    }
}
