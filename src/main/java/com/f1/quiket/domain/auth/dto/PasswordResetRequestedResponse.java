package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetRequestedResponse {

    private final String email;
    private final long expiresInSeconds;

    public static PasswordResetRequestedResponse of(String email, long expiresInSeconds) {
        return PasswordResetRequestedResponse.builder()
                .email(email)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
