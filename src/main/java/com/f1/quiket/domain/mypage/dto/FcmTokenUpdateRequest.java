package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenUpdateRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    @Size(max = 255, message = "FCM 토큰은 255자 이하여야 합니다")
    private String fcmToken;
}
