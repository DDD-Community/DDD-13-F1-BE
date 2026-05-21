package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoNicknameRequiredResponse {

    private final String signupToken;
    private final String provider;
    private final String suggestedNickname;

    public static KakaoNicknameRequiredResponse of(String signupToken, String suggestedNickname) {
        return KakaoNicknameRequiredResponse.builder()
                .signupToken(signupToken)
                .provider("kakao")
                .suggestedNickname(suggestedNickname)
                .build();
    }
}
