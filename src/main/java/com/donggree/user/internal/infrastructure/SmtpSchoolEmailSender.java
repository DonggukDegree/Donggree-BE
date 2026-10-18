package com.donggree.user.internal.infrastructure;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.SchoolEmailSender;
import com.donggree.user.internal.application.exception.SchoolEmailErrorCode;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpSchoolEmailSender implements SchoolEmailSender {
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String from;

    public SmtpSchoolEmailSender(
            ObjectProvider<JavaMailSender> senders,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from) {
        this.senders = senders;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public void send(String email, String code) {
        JavaMailSender sender = senders.getIfAvailable();
        if (!enabled || from.isBlank() || sender == null) {
            throw new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, "동그리");
            helper.setTo(email);
            helper.setSubject("[동그리] 학교 이메일 인증 코드");
            helper.setText("학교 이메일 인증 코드는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.\n본인이 요청하지 않았다면 이 메일을 무시해주세요.");
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            // SMTP 예외에는 주소·서버 정보가 포함될 수 있어 외부 응답이나 공용 예외 로그에 전달하지 않는다.
            throw new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        }
    }
}
