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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "member")
@SQLRestriction("deleted_at IS NULL")
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

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Member(String oauthId, String email) {
        this.oauthId = requireText(oauthId, "oauthId", 255);
        this.email = requireText(email, "email", 50);
        this.role = Role.STUDENT;
        this.identityVerified = false;
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

    /**
     * 프로필 정보를 수정한다.
     * 온보딩 미완료 상태이면 {@link IllegalStateException}을 던진다.
     * 본인 인증이 완료된 상태에서 학번 또는 이름을 변경하려 하면 {@link IllegalStateException}을 던진다.
     */
    public void updateProfile(String studentId, String name, String nickname) {
        if (!hasCompletedOnboarding()) {
            throw new IllegalStateException("온보딩을 먼저 완료해야 합니다.");
        }

        String validatedStudentId = requireText(studentId, "studentId", 10);
        String validatedName = requireText(name, "name", 5);
        String validatedNickname = requireText(nickname, "nickname", 8);

        if (identityVerified
            && (!this.studentId.equals(validatedStudentId)
            || !this.name.equals(validatedName))) {
            throw new IllegalStateException("본인 인증 완료 후에는 학번과 이름을 변경할 수 없습니다.");
        }

        this.studentId = validatedStudentId;
        this.name = validatedName;
        this.nickname = validatedNickname;
    }

    /**
     * 본인 인증을 완료 처리한다. transcript 모듈의 PDF 인증 이벤트 수신 시 사용된다.
     */
    public void verifyIdentity() {
        this.identityVerified = true;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    /**
     * 회원 탈퇴를 처리한다.
     * deletedAt을 현재 시각으로 기록하고, refreshToken과 studentId를 초기화한다.
     * studentId를 null로 초기화하여 unique 제약 충돌을 방지한다.
     * 이미 탈퇴한 회원이면 {@link IllegalStateException}을 던진다.
     */
    public void withdraw() {
        if (isDeleted()) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }
        this.deletedAt = LocalDateTime.now();
        this.refreshToken = null;
        this.studentId = null;
    }

    /**
     * 탈퇴한 회원을 재활성화한다.
     * deletedAt을 해제하고 프로필 정보를 초기 상태로 복원하여 온보딩부터 다시 시작하도록 한다.
     * 활성 상태의 회원에게 호출하면 {@link IllegalStateException}을 던진다.
     */
    public void reactivate() {
        if (!isDeleted()) {
            throw new IllegalStateException("탈퇴하지 않은 회원은 재활성화할 수 없습니다.");
        }
        this.deletedAt = null;
        this.studentId = null;
        this.name = null;
        this.nickname = null;
        this.profileUrl = null;
        this.identityVerified = false;
    }

    public boolean isDeleted() {
        return deletedAt != null;
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
