package com.f1.quiket.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "Kakao Access Token은 필수입니다")
    private String kakaoAccessToken;

    private Boolean agreedToTerms;
}
