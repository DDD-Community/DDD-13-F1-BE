package com.f1.quiket.global.auth;

import com.f1.quiket.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String publicId;
    private final String email;
    private final String status;

    private UserPrincipal(User user) {
        this.userId = user.getId();
        this.publicId = user.getPublicId();
        this.email = user.getEmail();
        this.status = user.getStatus();
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return publicId;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"locked".equals(status);
    }
}
