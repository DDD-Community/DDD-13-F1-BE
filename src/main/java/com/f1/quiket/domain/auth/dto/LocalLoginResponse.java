package com.f1.quiket.domain.auth.dto;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.user.entity.User;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocalLoginResponse {

    private final AuthUserResponse user;

    public static LocalLoginResponse of(User user, List<UserAuthIdentity> identities) {
        return LocalLoginResponse.builder()
                .user(AuthUserResponse.of(user, identities))
                .build();
    }
}
