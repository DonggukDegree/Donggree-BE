package com.donggree.user.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.JwtTokenProvider;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * 카카오 로그인 직후 상태(온보딩 미완료)의 테스트 멤버를 생성하고 액세스 토큰을 발급한다.
     * 같은 email로 재호출하면 기존 멤버를 재사용하며, soft-delete 상태면 재활성화한다.
     */
    @Transactional
    @PostMapping("/test-login")
    public ApiResponse<TestLoginResponse> testLogin(@RequestParam(defaultValue = "test@donggree.com") String email) {
        String targetEmail = (email.isBlank()) ? "test@donggree.com" : email;
        String oauthId = "test_" + targetEmail;

        Member member = memberRepository
                .findByOauthIdIncludingDeleted(oauthId)
                .map(m -> {
                    if (m.isDeleted()) m.reactivate();
                    return m;
                })
                .orElseGet(() -> memberRepository.save(Member.registerKakaoMember(oauthId, targetEmail)));

        String token = jwtTokenProvider.generateAccessToken(member.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, new TestLoginResponse(member.getId(), token));
    }

    public record TestLoginResponse(Long memberId, String accessToken) {}
}
