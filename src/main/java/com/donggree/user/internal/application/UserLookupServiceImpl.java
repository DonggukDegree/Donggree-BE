package com.donggree.user.internal.application;

import com.donggree.user.UserLookupService;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isStudentVerified(Long memberId) {
        return memberRepository
                .findById(memberId)
                .map(Member::isStudentVerified)
                .orElse(false);
    }
}
