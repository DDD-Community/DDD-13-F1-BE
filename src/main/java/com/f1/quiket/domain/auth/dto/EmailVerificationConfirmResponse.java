package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationConfirmResponse {

    private final String email;
    private final boolean verified;

    public static EmailVerificationConfirmResponse verified(String email) {
        return EmailVerificationConfirmResponse.builder()
                .email(email)
                .verified(true)
                .build();
    }
}
