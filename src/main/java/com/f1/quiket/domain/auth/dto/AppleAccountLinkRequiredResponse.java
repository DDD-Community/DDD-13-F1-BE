package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppleAccountLinkRequiredResponse {

    private final String email;
    private final String provider;
    private final String linkToken;
    private final long expiresInSeconds;

    public static AppleAccountLinkRequiredResponse of(String email, String linkToken, long expiresInSeconds) {
        return AppleAccountLinkRequiredResponse.builder()
                .email(email)
                .provider("apple")
                .linkToken(linkToken)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
