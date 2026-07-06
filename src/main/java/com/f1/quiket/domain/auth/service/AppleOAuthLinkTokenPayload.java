package com.f1.quiket.domain.auth.service;

public record AppleOAuthLinkTokenPayload(
        String providerSubject,
        String email,
        String oauthRefreshToken
) {
}
