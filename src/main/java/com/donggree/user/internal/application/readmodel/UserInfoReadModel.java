package com.donggree.user.internal.application.readmodel;

import com.donggree.user.internal.domain.Member;

/**
 * 회원 정보 조회(읽기) 모델. 응용 계층이 반환하며, 컨트롤러가 응답 DTO로 매핑한다.
 */
public record UserInfoReadModel(
        String studentId, String name, String nickname, boolean identityVerified, boolean schoolEmailVerified) {
    public static UserInfoReadModel from(Member member) {
        return new UserInfoReadModel(
                member.getStudentId(),
                member.getName(),
                member.getNickname(),
                member.isIdentityVerified(),
                member.isSchoolEmailVerified());
    }
}
