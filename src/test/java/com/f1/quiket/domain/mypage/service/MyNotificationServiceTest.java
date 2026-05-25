package com.f1.quiket.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.mypage.dto.FcmTokenUpdateRequest;
import com.f1.quiket.domain.mypage.dto.NotificationSettingsResponse;
import com.f1.quiket.domain.mypage.dto.NotificationSettingsUpdateRequest;
import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import com.f1.quiket.domain.mypage.repository.UserNotificationSettingRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MyNotificationServiceTest {

    private UserRepository userRepository;
    private UserNotificationSettingRepository userNotificationSettingRepository;
    private MyNotificationService myNotificationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userNotificationSettingRepository = mock(UserNotificationSettingRepository.class);
        myNotificationService = new MyNotificationService(userRepository, userNotificationSettingRepository);
    }

    @Test
    void getNotificationSettings_returns_default_without_insert_when_missing() {
        User user = user();
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userNotificationSettingRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        NotificationSettingsResponse response = myNotificationService.getNotificationSettings(user.getPublicId());

        assertThat(response.isFcmTokenRegistered()).isFalse();
        assertThat(response.isActivityEnabled()).isTrue();
        assertThat(response.isUpdateEnabled()).isTrue();
        assertThat(response.isReviewEnabled()).isTrue();
        // GET은 read-only — 첫 호출에도 INSERT 발생하지 않아야 함
        verify(userNotificationSettingRepository, never()).save(any(UserNotificationSetting.class));
    }

    @Test
    void getNotificationSettings_returns_existing_settings() {
        User user = user();
        UserNotificationSetting setting = UserNotificationSetting.createDefault(user.getId());
        setting.updateSettings(false, true, false);
        setting.updateFcmToken("fcm-token");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userNotificationSettingRepository.findByUserId(user.getId())).thenReturn(Optional.of(setting));

        NotificationSettingsResponse response = myNotificationService.getNotificationSettings(user.getPublicId());

        assertThat(response.isFcmTokenRegistered()).isTrue();
        assertThat(response.isActivityEnabled()).isFalse();
        assertThat(response.isUpdateEnabled()).isTrue();
        assertThat(response.isReviewEnabled()).isFalse();
    }

    @Test
    void updateNotificationSettings_updates_existing_settings() {
        User user = user();
        UserNotificationSetting setting = UserNotificationSetting.createDefault(user.getId());
        NotificationSettingsUpdateRequest request = notificationSettingsUpdateRequest(false, false, true);

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userNotificationSettingRepository.findByUserId(user.getId())).thenReturn(Optional.of(setting));

        NotificationSettingsResponse response = myNotificationService.updateNotificationSettings(
                user.getPublicId(),
                request
        );

        assertThat(setting.isActivityEnabled()).isFalse();
        assertThat(setting.isUpdateEnabled()).isFalse();
        assertThat(setting.isReviewEnabled()).isTrue();
        assertThat(response.isActivityEnabled()).isFalse();
        assertThat(response.isUpdateEnabled()).isFalse();
        assertThat(response.isReviewEnabled()).isTrue();
    }

    @Test
    void updateFcmToken_updates_existing_token() {
        User user = user();
        UserNotificationSetting setting = UserNotificationSetting.createDefault(user.getId());
        FcmTokenUpdateRequest request = fcmTokenUpdateRequest("new-fcm-token");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userNotificationSettingRepository.findByUserId(user.getId())).thenReturn(Optional.of(setting));

        myNotificationService.updateFcmToken(user.getPublicId(), request);

        assertThat(setting.getFcmToken()).isEqualTo("new-fcm-token");
    }

    @Test
    void updateFcmToken_creates_default_when_setting_missing() {
        User user = user();
        FcmTokenUpdateRequest request = fcmTokenUpdateRequest("new-fcm-token");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userNotificationSettingRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userNotificationSettingRepository.save(any(UserNotificationSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        myNotificationService.updateFcmToken(user.getPublicId(), request);

        ArgumentCaptor<UserNotificationSetting> settingCaptor =
                ArgumentCaptor.forClass(UserNotificationSetting.class);
        verify(userNotificationSettingRepository).save(settingCaptor.capture());
        assertThat(settingCaptor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(settingCaptor.getValue().getFcmToken()).isEqualTo("new-fcm-token");
    }

    @Test
    void getNotificationSettings_throws_not_found_when_user_missing() {
        String userPublicId = "018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901";
        when(userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myNotificationService.getNotificationSettings(userPublicId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    private User user() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private NotificationSettingsUpdateRequest notificationSettingsUpdateRequest(
            boolean activityEnabled,
            boolean updateEnabled,
            boolean reviewEnabled
    ) {
        NotificationSettingsUpdateRequest request = new NotificationSettingsUpdateRequest();
        ReflectionTestUtils.setField(request, "activityEnabled", activityEnabled);
        ReflectionTestUtils.setField(request, "updateEnabled", updateEnabled);
        ReflectionTestUtils.setField(request, "reviewEnabled", reviewEnabled);
        return request;
    }

    private FcmTokenUpdateRequest fcmTokenUpdateRequest(String fcmToken) {
        FcmTokenUpdateRequest request = new FcmTokenUpdateRequest();
        ReflectionTestUtils.setField(request, "fcmToken", fcmToken);
        return request;
    }
}
