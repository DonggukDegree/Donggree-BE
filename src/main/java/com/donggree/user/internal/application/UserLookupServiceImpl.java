package com.donggree.user.internal.application;

import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /** 탈퇴 회원은 엔티티의 삭제 제외 조건으로 조회되지 않아 결과에서 빠진다. */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, MemberProfile> findProfiles(Collection<Long> memberIds) {
        var ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(ids).stream()
                .map(member -> new MemberProfile(member.getId(), member.getNickname(), member.getProfileUrl()))
                .collect(Collectors.toMap(MemberProfile::memberId, Function.identity()));
    }
}
