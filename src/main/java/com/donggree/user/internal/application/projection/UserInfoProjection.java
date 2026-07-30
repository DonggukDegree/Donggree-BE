package com.donggree.user.internal.application.projection;

import com.donggree.user.internal.domain.Member;

/**
 * 회원 정보 조회(읽기) 프로젝션. 응용 계층이 반환하며, 컨트롤러가 응답 DTO로 매핑한다.
 */
public record UserInfoProjection(String studentId, String name, String nickname, boolean identityVerified) {
    public static UserInfoProjection from(Member member) {
        return new UserInfoProjection(
                member.getStudentId(), member.getName(), member.getNickname(), member.isIdentityVerified());
    }
}
