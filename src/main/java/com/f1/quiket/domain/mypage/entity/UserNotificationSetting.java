package com.f1.quiket.domain.mypage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "user_notification_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_notification_settings_user_id", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "fcm_token", length = 255)
    String fcmToken;

    @Column(name = "is_activity_enabled", nullable = false)
    boolean activityEnabled = true;

    @Column(name = "is_update_enabled", nullable = false)
    boolean updateEnabled = true;

    @Column(name = "is_review_enabled", nullable = false)
    boolean reviewEnabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    public static UserNotificationSetting createDefault(Long userId) {
        UserNotificationSetting setting = new UserNotificationSetting();
        setting.userId = userId;
        return setting;
    }

    public void updateSettings(boolean activityEnabled, boolean updateEnabled, boolean reviewEnabled) {
        this.activityEnabled = activityEnabled;
        this.updateEnabled = updateEnabled;
        this.reviewEnabled = reviewEnabled;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
