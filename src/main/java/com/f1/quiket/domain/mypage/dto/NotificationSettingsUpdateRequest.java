package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingsUpdateRequest {

    @NotNull(message = "활동 알림 설정은 필수입니다")
    private Boolean activityEnabled;

    @NotNull(message = "업데이트 알림 설정은 필수입니다")
    private Boolean updateEnabled;

    @NotNull(message = "복습 알림 설정은 필수입니다")
    private Boolean reviewEnabled;
}
