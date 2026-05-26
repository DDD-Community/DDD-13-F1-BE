package com.f1.quiket.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import com.f1.quiket.domain.mypage.repository.UserNotificationSettingRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UserNotificationSettingCreatorTest {

    private UserNotificationSettingRepository userNotificationSettingRepository;
    private UserNotificationSettingCreator userNotificationSettingCreator;

    @BeforeEach
    void setUp() {
        userNotificationSettingRepository = mock(UserNotificationSettingRepository.class);
        userNotificationSettingCreator = new UserNotificationSettingCreator(userNotificationSettingRepository);
    }

    @Test
    void createIfAbsent_returns_saved_setting_when_insert_succeeds() {
        Long userId = 1L;
        when(userNotificationSettingRepository.saveAndFlush(any(UserNotificationSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserNotificationSetting result = userNotificationSettingCreator.createIfAbsent(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isActivityEnabled()).isTrue();
        assertThat(result.isUpdateEnabled()).isTrue();
        assertThat(result.isReviewEnabled()).isTrue();
    }

    @Test
    void createIfAbsent_recovers_via_refetch_when_unique_constraint_violated() {
        // 다른 트랜잭션이 먼저 INSERT한 상태를 시뮬레이션
        Long userId = 1L;
        UserNotificationSetting concurrentlyCreatedSetting = UserNotificationSetting.createDefault(userId);

        when(userNotificationSettingRepository.saveAndFlush(any(UserNotificationSetting.class)))
                .thenThrow(new DataIntegrityViolationException("uq_user_notification_settings_user_id"));
        when(userNotificationSettingRepository.findByUserId(userId))
                .thenReturn(Optional.of(concurrentlyCreatedSetting));

        UserNotificationSetting result = userNotificationSettingCreator.createIfAbsent(userId);

        assertThat(result).isSameAs(concurrentlyCreatedSetting);
        verify(userNotificationSettingRepository).findByUserId(userId);
    }

    @Test
    void createIfAbsent_throws_internal_error_when_refetch_returns_empty() {
        // 매우 드문 케이스 — INSERT 실패 후 재조회도 비어있으면 일관성 깨진 상태
        Long userId = 1L;
        when(userNotificationSettingRepository.saveAndFlush(any(UserNotificationSetting.class)))
                .thenThrow(new DataIntegrityViolationException("uq_user_notification_settings_user_id"));
        when(userNotificationSettingRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userNotificationSettingCreator.createIfAbsent(userId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
