package com.f1.quiket.domain.auth.service;

public record AppleOAuthSignupTokenPayload(
        String providerSubject,
        String email,
        String suggestedNickname,
        String oauthRefreshToken
) {
}
