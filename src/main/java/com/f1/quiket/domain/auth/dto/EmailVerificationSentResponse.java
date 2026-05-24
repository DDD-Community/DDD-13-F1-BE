package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationSentResponse {

    private final String email;
    private final long expiresInSeconds;

    public static EmailVerificationSentResponse of(String email, long expiresInSeconds) {
        return EmailVerificationSentResponse.builder()
                .email(email)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
