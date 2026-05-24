package com.f1.quiket.domain.auth.dto;

import com.f1.quiket.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

    private final String userId;
    private final String email;
    private final String nickname;
    private final boolean emailVerificationRequired;
    private final boolean emailVerificationSent;

    public static SignupResponse of(User user, boolean emailVerificationSent) {
        return SignupResponse.builder()
                .userId(user.getPublicId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .emailVerificationRequired(true)
                .emailVerificationSent(emailVerificationSent)
                .build();
    }
}
