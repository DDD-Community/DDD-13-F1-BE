CREATE TABLE user_feedbacks (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (인증 엔드포인트 — 항상 존재)',
    category VARCHAR(20) NOT NULL COMMENT '카테고리: feature / bug / inquiry / other',
    body VARCHAR(1000) NOT NULL COMMENT '본문',
    reply_email VARCHAR(255) NULL COMMENT '회신 이메일',
    app_version VARCHAR(20) NULL COMMENT '앱 버전',
    os_version VARCHAR(50) NULL COMMENT 'OS 버전',
    device_model VARCHAR(100) NULL COMMENT '디바이스 모델',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '제출 시각',

    PRIMARY KEY (id),
    KEY idx_user_feedbacks_user_id (user_id),
    KEY idx_user_feedbacks_category_created_at (category, created_at),

    CONSTRAINT fk_user_feedbacks_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_feedbacks_category
        CHECK (category IN ('feature', 'bug', 'inquiry', 'other')),
    CONSTRAINT chk_user_feedbacks_body_length
        CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 피드백/문의';
