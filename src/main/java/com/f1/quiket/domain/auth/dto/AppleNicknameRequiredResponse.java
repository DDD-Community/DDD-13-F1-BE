package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppleNicknameRequiredResponse {

    private final String signupToken;
    private final String provider;
    private final String suggestedNickname;

    public static AppleNicknameRequiredResponse of(String signupToken, String suggestedNickname) {
        return AppleNicknameRequiredResponse.builder()
                .signupToken(signupToken)
                .provider("apple")
                .suggestedNickname(suggestedNickname)
                .build();
    }
}
