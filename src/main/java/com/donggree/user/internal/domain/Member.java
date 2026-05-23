package com.donggree.user.internal.domain;

import com.donggree.global.entity.BaseEntity;
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

    @Column(name = "profile_url", nullable = false, length = 512)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private static final String DEFAULT_PROFILE_URL =
            "https://k.kakaocdn.net/dn/dpk9l1/btqmGhA2lKL/Oz0wDuJn1YV2DIn92f6DVK/img_640x640.jpg";

    private Member(String oauthId, String email) {
        this.oauthId = requireText(oauthId, "oauthId", 255);
        this.email = requireText(email, "email", 50);
        this.profileUrl = DEFAULT_PROFILE_URL;
        this.role = Role.STUDENT;
    }

    public static Member registerKakaoMember(String oauthId, String email) {
        return new Member(oauthId, email);
    }

    public void completeOnboarding(String studentId) {
        this.studentId = requireText(studentId, "studentId", 10);
    }

    public boolean hasCompletedOnboarding() {
        return studentId != null;
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
