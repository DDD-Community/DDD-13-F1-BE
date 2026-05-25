CREATE TABLE IF NOT EXISTS user_xp_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    play_session_id BIGINT NULL COMMENT '풀이 세션 ID',
    lecture_upload_id BIGINT NULL COMMENT '업로드 ID',
    xp_type VARCHAR(30) NOT NULL COMMENT 'XP 유형',
    base_xp SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '기본 XP',
    streak_multiplier DECIMAL(3,1) NOT NULL DEFAULT 1.0 COMMENT '연속 학습 배수',
    earned_xp SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '실제 적립 XP',
    xp_before INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '적립 전 누적 XP',
    xp_after INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '적립 후 누적 XP',
    level_before TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '적립 전 레벨',
    level_after TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '적립 후 레벨',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '적립 시각',

    PRIMARY KEY (id),
    KEY idx_user_xp_logs_user_id_created_at (user_id, created_at),
    KEY idx_user_xp_logs_play_session_id (play_session_id),

    CONSTRAINT chk_user_xp_logs_xp_type
        CHECK (xp_type IN ('quiz_correct', 'quiz_retry', 'upload', 'streak_bonus'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XP 적립 이력';

CREATE TABLE IF NOT EXISTS user_dotori_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    play_session_id BIGINT NULL COMMENT '풀이 세션 ID',
    change_type VARCHAR(20) NOT NULL COMMENT '변동 유형',
    amount SMALLINT NOT NULL COMMENT '변동량',
    balance_before INT NOT NULL COMMENT '변동 전 잔액',
    balance_after INT NOT NULL COMMENT '변동 후 잔액',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '변동 시각',

    PRIMARY KEY (id),
    KEY idx_user_dotori_logs_user_id_created_at (user_id, created_at),
    KEY idx_user_dotori_logs_play_session_id (play_session_id),

    CONSTRAINT chk_user_dotori_logs_change_type
        CHECK (change_type IN ('earn_quiz', 'spend_item')),
    CONSTRAINT chk_user_dotori_logs_balance_after
        CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도토리 적립/차감 이력';

CREATE TABLE IF NOT EXISTS user_streak_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    study_date DATE NOT NULL COMMENT '학습 날짜',
    streak_count SMALLINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '연속 학습 일수',
    multiplier DECIMAL(3,1) NOT NULL DEFAULT 1.0 COMMENT '적용 배수',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '기록 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_streak_logs_user_study_date (user_id, study_date),
    KEY idx_user_streak_logs_user_id_study_date (user_id, study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연속 학습 이력';
