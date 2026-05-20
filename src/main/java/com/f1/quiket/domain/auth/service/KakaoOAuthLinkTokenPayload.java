package com.f1.quiket.domain.auth.service;

public record KakaoOAuthLinkTokenPayload(
        String providerSubject,
        String email
) {
}
