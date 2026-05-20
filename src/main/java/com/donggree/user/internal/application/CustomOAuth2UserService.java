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
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String DEFAULT_PROFILE_URL =
            "https://k.kakaocdn.net/dn/dpk9l1/btqmGhA2lKL/Oz0wDuJn1YV2DIn92f6DVK/img_640x640.jpg";

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오 응답에서 사용자 정보 추출
        String oauthId = String.valueOf(oAuth2User.getAttribute("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (kakaoAccount != null)
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;

        String email = (kakaoAccount != null) ? (String) kakaoAccount.get("email") : null;
        String nickname = (profile != null) ? (String) profile.get("nickname") : null;
        String profileImageUrl = (profile != null) ? (String) profile.get("profile_image_url") : null;

        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            profileImageUrl = DEFAULT_PROFILE_URL;
        }

        // DB 조회: oauthId로 기존 회원 검색, 없으면 신규 등록
        String finalProfileImageUrl = profileImageUrl;
        Member member = memberRepository.findByOauthId(oauthId)
                .orElseGet(() -> memberRepository.save(
                        Member.registerKakaoMember(oauthId, email, nickname, finalProfileImageUrl)
                ));

        return new OAuthMember(member, oAuth2User.getAttributes());
    }
}
