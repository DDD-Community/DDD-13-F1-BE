package com.f1.quiket.infra.kakao.dto;

import org.springframework.util.StringUtils;

public record KakaoUserInfo(
        String providerSubject,
        String email,
        Boolean emailValid,
        Boolean emailVerified,
        String nickname
) {

    public boolean hasUsableEmail() {
        return StringUtils.hasText(email)
                && Boolean.TRUE.equals(emailValid)
                && Boolean.TRUE.equals(emailVerified);
    }
}
