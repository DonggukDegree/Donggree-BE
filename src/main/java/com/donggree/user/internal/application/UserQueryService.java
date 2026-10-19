package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.application.readmodel.SchoolEmailVerificationReadModel;
import com.donggree.user.internal.application.readmodel.UserInfoReadModel;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 조회(Query) 응용 서비스.
 * 상태를 변경하지 않는 읽기 유스케이스만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final MemberRepository memberRepository;
    private final SchoolEmailVerificationStore emailVerifications;

    @Transactional(readOnly = true)
    public SchoolEmailVerificationReadModel getSchoolEmailVerification(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));
        return SchoolEmailVerificationReadModel.from(
                member, emailVerifications.findById(memberId).orElse(null));
    }

    /**
     * 로그인한 회원의 정보를 조회한다.
     *
     * @param memberId 로그인한 회원 ID
     * @return 학번, 이름, 닉네임이 담긴 응답
     * @throws GeneralException 회원 미존재 시
     */
    @Transactional(readOnly = true)
    public UserInfoReadModel getUserInfo(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));

        return UserInfoReadModel.from(member);
    }
}
