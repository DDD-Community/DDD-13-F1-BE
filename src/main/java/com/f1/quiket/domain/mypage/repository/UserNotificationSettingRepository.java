package com.f1.quiket.domain.mypage.repository;

import com.f1.quiket.domain.mypage.entity.UserNotificationSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    Optional<UserNotificationSetting> findByUserId(Long userId);
}
