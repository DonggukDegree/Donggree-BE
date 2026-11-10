package com.donggree.user.internal.infrastructure;

import com.donggree.user.internal.domain.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 인증 완료 후 SecurityContext에 저장되는 인증 객체.
 * Spring Security의 {@link OAuth2User}를 구현하며, 내부에 {@link Member} 엔티티를 보유한다.
 * OAuthSuccessHandler에서 Member 정보를 꺼내 JWT를 생성하는 데 사용된다.
 */
@Getter
public class OAuthMember implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;

    public OAuthMember(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getName() {
        return String.valueOf(member.getId());
    }
}
