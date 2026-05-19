package com.f1.quiket.domain.auth.dto;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.user.entity.User;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserResponse {

    private final String id;
    private final String email;
    private final String nickname;
    private final Integer dotoriBalance;
    private final boolean emailVerified;
    private final String status;
    private final List<String> providers;

    public static AuthUserResponse of(User user, List<UserAuthIdentity> identities) {
        return AuthUserResponse.builder()
                .id(user.getPublicId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .dotoriBalance(user.getDotoriBalance())
                .emailVerified(user.isEmailVerified())
                .status(user.getStatus())
                .providers(identities.stream()
                        .map(UserAuthIdentity::getProvider)
                        .toList())
                .build();
    }
}
