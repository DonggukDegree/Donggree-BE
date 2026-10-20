package com.donggree.user.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.exception.SchoolEmailErrorCode;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpSchoolEmailSenderTest {
    @Test
    void 중첩된_SMTP_타임아웃을_원문없이_구분한다() {
        when(providers.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var nested = new jakarta.mail.MessagingException(
                "private SMTP host", new java.net.SocketTimeoutException("private address"));
        doThrow(new MailSendException("private address", nested)).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> new SmtpSchoolEmailSender(providers, true, "noreply@example.invalid")
                        .send("student@dgu.ac.kr", "012345"))
                .isInstanceOf(com.donggree.user.internal.application.exception.SchoolEmailSendException.class)
                .hasNoCause()
                .hasMessageNotContaining("private")
                .satisfies(error -> assertThat(
                                ((com.donggree.user.internal.application.exception.SchoolEmailSendException) error)
                                        .getReason())
                        .isEqualTo(
                                com.donggree.user.internal.application.exception.SchoolEmailSendException.Reason
                                        .TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> providers = mock(ObjectProvider.class);

    private final JavaMailSender sender = mock(JavaMailSender.class);

    @Test
    void 코드와_고정학교주소를_메일에_담아_발송한다() throws Exception {
        when(providers.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        new SmtpSchoolEmailSender(providers, true, "noreply@example.invalid").send("student@dgu.ac.kr", "012345");
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        assertThat(captured.getValue().getAllRecipients()[0].toString()).isEqualTo("student@dgu.ac.kr");
        assertThat(captured.getValue().getContent().toString()).contains("012345", "5분");
    }

    @Test
    void 비활성화된_발송은_성공으로_처리하지_않는다() {
        assertThatThrownBy(() -> new SmtpSchoolEmailSender(providers, false, "").send("student@dgu.ac.kr", "012345"))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode())
                        .isEqualTo(SchoolEmailErrorCode.SEND_UNAVAILABLE));
        verifyNoInteractions(sender);
    }

    @Test
    void SMTP_실패의_내부주소나_메시지를_외부예외에_포함하지_않는다() {
        when(providers.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("internal SMTP detail")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> new SmtpSchoolEmailSender(providers, true, "noreply@example.invalid")
                        .send("student@dgu.ac.kr", "012345"))
                .isInstanceOf(GeneralException.class)
                .hasNoCause()
                .hasMessageNotContaining("internal SMTP detail");
    }
}
