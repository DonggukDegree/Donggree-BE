package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.presentation.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 관리를 담당하는 응용 서비스.
 * 온보딩, 프로필 조회/수정 등 사용자 관련 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final MemberRepository memberRepository;

    /**
     * 온보딩을 완료하고 회원의 학번과 이름을 설정한다.
     *
     * @param memberId  로그인한 회원 ID
     * @param studentId 학번
     * @param name      이름
     * @throws GeneralException 회원 미존재, 이미 온보딩 완료, 학번 중복 시
     */
    @Transactional
    public void completeOnboarding(Long memberId, String studentId, String name) {
        String trimmedStudentId = studentId.trim();
        String trimmedName = name.trim();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));

        if (member.hasCompletedOnboarding()) {
            throw new GeneralException(UserErrorCode.ALREADY_ONBOARDED);
        }

        if (memberRepository.existsByStudentId(trimmedStudentId)) {
            throw new GeneralException(UserErrorCode.DUPLICATE_STUDENT_ID);
        }

        member.completeOnboarding(trimmedStudentId, trimmedName);
    }

    /**
     * 로그인한 회원의 정보를 조회한다.
     *
     * @param memberId 로그인한 회원 ID
     * @return 학번, 이름, 닉네임이 담긴 응답
     * @throws GeneralException 회원 미존재 시
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));

        return new UserInfoResponse(
                member.getStudentId(),
                member.getName(),
                member.getNickname()
        );
    }
}
