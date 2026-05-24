package com.donggree.user.internal.application;

import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카카오 OAuth2 인증 후 사용자 정보를 처리하는 서비스.
 * Spring Security가 카카오 API에서 사용자 정보를 가져온 뒤 이 메서드를 호출한다.
 * 기존 회원이면 조회, 신규 회원이면 등록한 뒤 {@link OAuthMember}로 감싸 반환한다.
 * 카카오에서 oauthId와 email만 추출하며, 이름·닉네임은 온보딩에서 별도로 입력받는다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오 응답에서 oauthId, email만 추출
        String oauthId = String.valueOf(oAuth2User.getAttribute("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        String email = (kakaoAccount != null) ? (String) kakaoAccount.get("email") : null;

        // 탈퇴 회원 포함 전체 조회 → 탈퇴 상태면 재활성화 → 없으면 신규 등록
        Member member = memberRepository.findByOauthIdIncludingDeleted(oauthId)
                .map(m -> {
                    if (m.isDeleted()) {
                        m.reactivate();
                    }
                    return m;
                })
                .orElseGet(() -> memberRepository.save(
                        Member.registerKakaoMember(oauthId, email)
                ));

        return new OAuthMember(member, oAuth2User.getAttributes());
    }
}
