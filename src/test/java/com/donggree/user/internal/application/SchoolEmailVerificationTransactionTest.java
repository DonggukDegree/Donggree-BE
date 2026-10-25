package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.user.internal.application.command.RequestSchoolEmailCodeCommand;
import com.donggree.user.internal.application.command.VerifySchoolEmailCodeCommand;
import com.donggree.user.internal.application.exception.SchoolEmailErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.SchoolEmailDeliveryStatus;
import com.donggree.user.internal.domain.SchoolEmailVerification;
import com.donggree.user.internal.infrastructure.InMemorySchoolEmailMailQueue;
import com.donggree.user.internal.infrastructure.InMemorySchoolEmailVerificationStore;
import com.donggree.user.internal.infrastructure.SecureSchoolEmailCodes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 서비스 프록시·트랜잭션으로 코드 실패 횟수와 비동기 메일 발송 상태를 검증한다. */
@DataJpaTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:school-email-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "app.school-email.code-secret=school-email-test-secret-only",
            "app.mail.enabled=true",
            "spring.jpa.show-sql=false"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    SchoolEmailVerificationCommandService.class,
    InMemorySchoolEmailMailQueue.class,
    InMemorySchoolEmailVerificationStore.class,
    SchoolEmailMailDeliveryService.class,
    SecureSchoolEmailCodes.class,
    JpaAuditingConfig.class,
    UserQueryService.class,
    UserCommandService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchoolEmailVerificationTransactionTest {
    @Autowired
    private SchoolEmailVerificationCommandService service;

    @Autowired
    private UserQueryService queries;

    @Autowired
    private UserCommandService users;

    @Autowired
    private MemberRepository members;

    @Autowired
    private SchoolEmailVerificationStore verifications;

    @Autowired
    private PlatformTransactionManager transactions;

    @Autowired
    private SchoolEmailMailDeliveryService mailDelivery;

    @Autowired
    private InMemorySchoolEmailMailQueue mailQueue;

    @MockitoBean
    private SchoolEmailSender sender;

    @MockitoBean
    private SchoolEmailTelemetry telemetry;

    private Long memberId;
    private String oauthId;
    private final AtomicReference<String> sentCode = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        mailQueue.open();
        oauthId = UUID.randomUUID().toString();
        Member member = Member.registerKakaoMember(oauthId, "test@example.invalid");
        member.completeOnboarding("2026000001", "테스트");
        memberId = members.saveAndFlush(member).getId();
        doAnswer(invocation -> {
                    sentCode.set(invocation.getArgument(1));
                    return null;
                })
                .when(sender)
                .send(anyString(), anyString());
    }

    @AfterEach
    void cleanup() {
        verifications.deleteById(memberId);
        mailQueue.cancel(memberId);
        members.deleteAll();
    }

    @Test
    void 학교이메일을_인증하고_PDF_본인인증과_카카오이메일은_보존한다() {
        var response = request("student");
        assertThat(response.email()).isEqualTo("student@dgu.ac.kr");
        assertThat(sentCode.get()).matches("[0-9]{6}");
        assertThat(verifications.findById(memberId).orElseThrow().getCodeHash())
                .hasSize(64)
                .isNotEqualTo(sentCode.get());
        confirm("student", sentCode.get());
        var member = members.findById(memberId).orElseThrow();
        assertThat(member.isSchoolEmailVerified()).isTrue();
        assertThat(member.getSchoolEmail()).isEqualTo("student@dgu.ac.kr");
        assertThat(member.getSchoolEmailVerifiedAt()).isNotNull();
        assertThat(member.isIdentityVerified()).isFalse();
        assertThat(member.getEmail()).isEqualTo("test@example.invalid");
        assertThat(verifications.findById(memberId)).isEmpty();
        assertThat(queries.getSchoolEmailVerification(memberId).schoolEmailVerified())
                .isTrue();
        assertError(() -> confirm("student", sentCode.get()), SchoolEmailErrorCode.ALREADY_VERIFIED);
    }

    @Test
    void 잘못된_코드의_실패횟수는_응답이_실패해도_저장되고_다섯번이면_잠긴다() {
        request("student");
        String incorrect = sentCode.get().equals("000000") ? "000001" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            var error = attempt == 5 ? SchoolEmailErrorCode.ATTEMPT_LIMIT : SchoolEmailErrorCode.INVALID_CODE;
            assertError(() -> confirm("student", incorrect), error);
            assertThat(verifications.findById(memberId).orElseThrow().getAttempts())
                    .isEqualTo(attempt);
        }
        assertError(() -> confirm("student", sentCode.get()), SchoolEmailErrorCode.ATTEMPT_LIMIT);
        assertThat(members.findById(memberId).orElseThrow().isSchoolEmailVerified())
                .isFalse();
    }

    @Test
    void 다른_아이디로_같은_코드를_사용할_수_없다() {
        request("student");
        assertError(() -> confirm("another", sentCode.get()), SchoolEmailErrorCode.INVALID_CODE);
        confirm("student", sentCode.get());
    }

    @Test
    void 다른_회원은_발송받은_코드를_사용할_수_없다() {
        request("student");
        Member other = Member.registerKakaoMember("other", "other@example.invalid");
        other.completeOnboarding("2026000002", "다른이");
        Long otherId = members.saveAndFlush(other).getId();
        assertError(
                () -> service.verifyCode(otherId, new VerifySchoolEmailCodeCommand("student", sentCode.get())),
                SchoolEmailErrorCode.NOT_REQUESTED);
    }

    @Test
    void 주소를_바꿔_요청해도_60초_재발송_제한이_적용된다() {
        request("student");
        assertError(() -> request("another"), SchoolEmailErrorCode.RESEND_LIMIT);
    }

    @Test
    void 재발송하면_이전_주소의_코드는_사용할_수_없다() {
        request("student");
        String original = sentCode.get();
        changeVerification(v -> ReflectionTestUtils.setField(v, "sentAt", now().minusMinutes(2)));
        request("another");
        assertError(() -> confirm("student", original), SchoolEmailErrorCode.INVALID_CODE);
        confirm("another", sentCode.get());
    }

    @Test
    void 만료된_코드와_일일발송제한을_검사한다() {
        request("student");
        changeVerification(v -> {
            ReflectionTestUtils.setField(v, "expiresAt", now().minusSeconds(1));
            ReflectionTestUtils.setField(v, "sentAt", now().minusMinutes(6));
            ReflectionTestUtils.setField(v, "dailySends", 10);
        });
        assertError(() -> confirm("student", sentCode.get()), SchoolEmailErrorCode.EXPIRED);
        assertError(() -> request("another"), SchoolEmailErrorCode.DAILY_LIMIT);
    }

    @Test
    void SMTP_실패는_실패상태를_저장하고_즉시_재발송을_허용한다() {
        request("student");
        changeVerification(v -> ReflectionTestUtils.setField(v, "sentAt", now().minusMinutes(2)));
        doThrow(new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE))
                .when(sender)
                .send(anyString(), anyString());
        request("another");
        var verification = verifications.findById(memberId).orElseThrow();
        assertThat(verification.getEmail()).isEqualTo("another@dgu.ac.kr");
        assertThat(verification.getDeliveryStatus()).isEqualTo(SchoolEmailDeliveryStatus.FAILED);
        assertThat(verification.canResend(now())).isTrue();
        assertError(() -> confirm("another", sentCode.get()), SchoolEmailErrorCode.SEND_UNAVAILABLE);
    }

    @Test
    void PDF_본인인증_회원도_학교이메일을_인증하고_두_상태를_모두_유지한다() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> members.findById(memberId).orElseThrow().verifyIdentity());
        assertThat(queries.getSchoolEmailVerification(memberId).identityVerified())
                .isTrue();
        request("student");
        confirm("student", sentCode.get());
        var member = members.findById(memberId).orElseThrow();
        assertThat(member.isIdentityVerified()).isTrue();
        assertThat(member.isSchoolEmailVerified()).isTrue();
        assertError(() -> request("student"), SchoolEmailErrorCode.ALREADY_VERIFIED);
    }

    @Test
    void 탈퇴하면_진행중인_인증요청과_완료정보를_정리한다() {
        request("student");
        users.deleteUser(memberId);
        assertThat(verifications.findById(memberId)).isEmpty();
        var deleted = members.findByOauthIdIncludingDeleted(oauthId).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getSchoolEmail()).isNull();
        assertThat(deleted.getSchoolEmailVerifiedAt()).isNull();
    }

    @Test
    void 허용되지_않은_도메인에는_발송하지_않는다() {
        assertError(() -> request("student@gmail.com"), SchoolEmailErrorCode.INVALID_EMAIL);
        verifyNoInteractions(sender);
    }

    @Test
    void 이미_다른_계정이_인증한_학교이메일은_다시_사용할_수_없다() {
        request("student");
        confirm("student", sentCode.get());
        Member other = Member.registerKakaoMember("duplicate-email-test", "other@example.invalid");
        other.completeOnboarding("2026000002", "다른이");
        Long otherId = members.saveAndFlush(other).getId();
        assertError(
                () -> service.requestCode(otherId, new RequestSchoolEmailCodeCommand("Student")),
                SchoolEmailErrorCode.EMAIL_IN_USE);
    }

    @Test
    void 인증완료_후_탈퇴하면_학교메일과_인증시각을_초기화한다() {
        request("student");
        confirm("student", sentCode.get());
        users.deleteUser(memberId);
        Member deleted = members.findByOauthIdIncludingDeleted(oauthId).orElseThrow();
        assertThat(deleted.getSchoolEmail()).isNull();
        assertThat(deleted.getSchoolEmailVerifiedAt()).isNull();
        deleted.reactivate();
        assertThat(deleted.isSchoolEmailVerified()).isFalse();
    }

    @Test
    void 먼저_읽은_회원의_로그인토큰_저장이_새_이메일인증을_덮어쓰지_않는다() {
        request("student");
        var outer = new TransactionTemplate(transactions);
        outer.executeWithoutResult(status -> {
            Member stale = members.findById(memberId).orElseThrow();
            var concurrent = new TransactionTemplate(transactions);
            concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            concurrent.executeWithoutResult(inner -> confirm("student", sentCode.get()));
            stale.updateRefreshToken("new-refresh-token");
        });
        Member member = members.findById(memberId).orElseThrow();
        assertThat(member.isSchoolEmailVerified()).isTrue();
        assertThat(member.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    private com.donggree.user.internal.application.command.SchoolEmailCodeRequestResult request(String localPart) {
        var response = service.requestCode(memberId, new RequestSchoolEmailCodeCommand(localPart));
        mailDelivery.deliverNext();
        return response;
    }

    private void confirm(String localPart, String code) {
        service.verifyCode(memberId, new VerifySchoolEmailCodeCommand(localPart, code));
    }

    private void changeVerification(Consumer<SchoolEmailVerification> change) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var verification = verifications.findById(memberId).orElseThrow();
            change.accept(verification);
            verifications.save(verification);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void assertError(Runnable action, SchoolEmailErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(GeneralException.class).satisfies(error -> assertThat(
                        ((GeneralException) error).getCode())
                .isEqualTo(code));
    }
}
