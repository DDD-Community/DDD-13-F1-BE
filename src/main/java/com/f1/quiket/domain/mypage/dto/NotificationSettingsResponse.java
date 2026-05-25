package com.f1.quiket.domain.mypage.dto;

import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
public class NotificationSettingsResponse {

    private final boolean fcmTokenRegistered;
    private final boolean activityEnabled;
    private final boolean updateEnabled;
    private final boolean reviewEnabled;

    public static NotificationSettingsResponse from(UserNotificationSetting setting) {
        return NotificationSettingsResponse.builder()
                .fcmTokenRegistered(StringUtils.hasText(setting.getFcmToken()))
                .activityEnabled(setting.isActivityEnabled())
                .updateEnabled(setting.isUpdateEnabled())
                .reviewEnabled(setting.isReviewEnabled())
                .build();
    }

    /**
     * 사용자 알림 설정이 아직 생성되지 않은 경우의 기본값 응답 (기능명세 MY-002 — 모두 ON)
     */
    public static NotificationSettingsResponse defaults() {
        return NotificationSettingsResponse.builder()
                .fcmTokenRegistered(false)
                .activityEnabled(true)
                .updateEnabled(true)
                .reviewEnabled(true)
                .build();
    }
}
