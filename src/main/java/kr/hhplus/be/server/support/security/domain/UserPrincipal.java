package kr.hhplus.be.server.support.security.domain;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import kr.hhplus.be.server.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security에서 사용할 사용자 인증 정보
 */
@Getter
@RequiredArgsConstructor
@Builder

public class UserPrincipal implements UserDetails {
    
    private final Long userId;
    private final String accountId;
    private final String password;
    private final Long point;

    public static UserPrincipal from(User user) {
        return UserPrincipal.builder()
            .userId(user.id())
            .accountId(user.accountId())
            .password(user.password())
            .point(user.point())
            .build();
    }

    // ========== UserDetails 구현 ==========

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한이 필요하면 여기서 설정
        // 예: return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return accountId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}