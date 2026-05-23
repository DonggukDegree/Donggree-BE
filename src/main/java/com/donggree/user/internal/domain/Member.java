package com.donggree.user.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.user.internal.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", unique = true, length = 10)
    private String studentId;

    @Column(length = 5)
    private String name;

    @Column(length = 8)
    private String nickname;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(name = "oauth_id", nullable = false, unique = true, length = 255)
    private String oauthId;

    @Column(name = "profile_url", length = 512)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "refresh_token", length = 1024)
    private String refreshToken;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Member(String oauthId, String email) {
        this.oauthId = requireText(oauthId, "oauthId", 255);
        this.email = requireText(email, "email", 50);
        this.role = Role.STUDENT;
    }

    public static Member registerKakaoMember(String oauthId, String email) {
        return new Member(oauthId, email);
    }

    /**
     * 온보딩을 완료하고 학번과 이름을 설정한다. 닉네임은 이름과 동일한 값으로 초기화한다.
     * 이미 온보딩이 완료된 상태라면 {@link IllegalStateException}을 던진다.
     */
    public void completeOnboarding(String studentId, String name) {
        if (hasCompletedOnboarding()) {
            throw new IllegalStateException("이미 온보딩이 완료된 회원입니다.");
        }
        String validatedStudentId = requireText(studentId, "studentId", 10);
        String validatedName = requireText(name, "name", 5);
        this.studentId = validatedStudentId;
        this.name = validatedName;
        this.nickname = this.name;
    }

    public boolean hasCompletedOnboarding() {
        return studentId != null;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }

        return trimmed;
    }

}
