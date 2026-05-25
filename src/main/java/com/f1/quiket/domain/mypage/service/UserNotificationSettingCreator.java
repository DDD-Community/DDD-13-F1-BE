package com.f1.quiket.domain.mypage.service;

import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import com.f1.quiket.domain.mypage.repository.UserNotificationSettingRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 설정 첫 생성 시 동시 요청 race를 방어하는 별도 트랜잭션 컴포넌트
 *
 * - {@code user_notification_settings.user_id} UNIQUE 제약 위반(동시 INSERT)을 catch
 * - REQUIRES_NEW로 격리된 트랜잭션 안에서만 INSERT/재조회가 일어나므로 호출 측 트랜잭션은 rollback-only 마크되지 않음
 */
@Component
@RequiredArgsConstructor
public class UserNotificationSettingCreator {

    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserNotificationSetting createIfAbsent(Long userId) {
        try {
            return userNotificationSettingRepository.saveAndFlush(UserNotificationSetting.createDefault(userId));
        } catch (DataIntegrityViolationException e) {
            return userNotificationSettingRepository.findByUserId(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 설정 조회 실패", e));
        }
    }
}
