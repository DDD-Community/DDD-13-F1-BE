package com.f1.quiket.infra.apple.dto;

import org.springframework.util.StringUtils;

public record AppleUserInfo(
        String providerSubject,
        String email,
        Boolean emailVerified
) {

    public boolean hasUsableEmail() {
        return StringUtils.hasText(email) && Boolean.TRUE.equals(emailVerified);
    }
}
