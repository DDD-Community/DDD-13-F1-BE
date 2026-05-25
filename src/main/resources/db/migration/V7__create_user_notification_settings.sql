CREATE TABLE user_notification_settings (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    fcm_token VARCHAR(255) NULL COMMENT 'FCM 푸시 토큰',
    is_activity_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활동 알림 여부',
    is_update_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '업데이트 알림 여부',
    is_review_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '복습 알림 여부',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_notification_settings_user_id (user_id),

    CONSTRAINT fk_user_notification_settings_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_notification_settings_activity_enabled
        CHECK (is_activity_enabled IN (0, 1)),
    CONSTRAINT chk_user_notification_settings_update_enabled
        CHECK (is_update_enabled IN (0, 1)),
    CONSTRAINT chk_user_notification_settings_review_enabled
        CHECK (is_review_enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 알림 설정';
