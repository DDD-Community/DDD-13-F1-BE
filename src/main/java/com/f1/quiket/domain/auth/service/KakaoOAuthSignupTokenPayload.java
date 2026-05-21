package com.f1.quiket.domain.auth.service;

public record KakaoOAuthSignupTokenPayload(
        String providerSubject,
        String email,
        String suggestedNickname
) {
}
