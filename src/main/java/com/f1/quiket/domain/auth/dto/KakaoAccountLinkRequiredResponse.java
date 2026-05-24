package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoAccountLinkRequiredResponse {

    private final String email;
    private final String provider;
    private final String linkToken;
    private final long expiresInSeconds;

    public static KakaoAccountLinkRequiredResponse of(String email, String linkToken, long expiresInSeconds) {
        return KakaoAccountLinkRequiredResponse.builder()
                .email(email)
                .provider("kakao")
                .linkToken(linkToken)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
