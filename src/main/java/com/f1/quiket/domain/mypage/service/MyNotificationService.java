package com.f1.quiket.domain.mypage.service;

import com.f1.quiket.domain.mypage.dto.FcmTokenUpdateRequest;
import com.f1.quiket.domain.mypage.dto.NotificationSettingsResponse;
import com.f1.quiket.domain.mypage.dto.NotificationSettingsUpdateRequest;
import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import com.f1.quiket.domain.mypage.repository.UserNotificationSettingRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyNotificationService {

    private final UserRepository userRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;

    public NotificationSettingsResponse getNotificationSettings(String userPublicId) {
        User user = findActiveUser(userPublicId);
        return NotificationSettingsResponse.from(findOrCreateSetting(user.getId()));
    }

    public NotificationSettingsResponse updateNotificationSettings(
            String userPublicId,
            NotificationSettingsUpdateRequest request
    ) {
        User user = findActiveUser(userPublicId);
        UserNotificationSetting setting = findOrCreateSetting(user.getId());
        setting.updateSettings(
                request.getActivityEnabled(),
                request.getUpdateEnabled(),
                request.getReviewEnabled()
        );
        return NotificationSettingsResponse.from(setting);
    }

    public void updateFcmToken(String userPublicId, FcmTokenUpdateRequest request) {
        User user = findActiveUser(userPublicId);
        UserNotificationSetting setting = findOrCreateSetting(user.getId());
        setting.updateFcmToken(request.getFcmToken());
    }

    private User findActiveUser(String userPublicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private UserNotificationSetting findOrCreateSetting(Long userId) {
        return userNotificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> userNotificationSettingRepository.save(UserNotificationSetting.createDefault(userId)));
    }
}
