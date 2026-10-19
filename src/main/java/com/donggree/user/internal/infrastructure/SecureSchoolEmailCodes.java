package com.donggree.user.internal.infrastructure;

import com.donggree.user.internal.application.SchoolEmailCodes;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 서버 비밀키를 사용해 인증 코드 해시 노출 시 오프라인 대입을 방지한다. */
@Component
public class SecureSchoolEmailCodes implements SchoolEmailCodes {
    private final SecureRandom random = new SecureRandom();
    private final byte[] secret;

    public SecureSchoolEmailCodes(@Value("${app.school-email.code-secret:${app.jwt.secret}}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }

    @Override
    public String hash(Long memberId, String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] bytes = (memberId + ":" + email + ":" + code).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(bytes));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("인증 코드 암호화 설정을 확인해주세요.", exception);
        }
    }

    @Override
    public boolean matches(Long memberId, String email, String code, String hash) {
        return MessageDigest.isEqual(
                hash(memberId, email, code).getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
    }
}
