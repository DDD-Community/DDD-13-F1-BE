CREATE TABLE quiz_generation_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    quiz_session_id BIGINT NOT NULL COMMENT '퀴즈 세션 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '작업 상태',
    progress_pct TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '진행률',
    estimated_seconds SMALLINT NULL COMMENT '예상 소요 시간',
    estimated_finish_at DATETIME(3) NULL COMMENT '예상 완료 시각',
    started_at DATETIME(3) NULL COMMENT '처리 시작 시각',
    completed_at DATETIME(3) NULL COMMENT '완료 시각',
    timeout_at DATETIME(3) NULL COMMENT '타임아웃 기준 시각',
    retry_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    is_retryable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '재시도 가능 여부',
    fail_code VARCHAR(50) NULL COMMENT '실패 코드',
    fail_reason VARCHAR(500) NULL COMMENT '실패 상세 사유',
    mq_message_id VARCHAR(100) NULL COMMENT 'Redis Stream message-id',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_generation_jobs_session_id (quiz_session_id),
    KEY idx_quiz_generation_jobs_user_id_status (user_id, status),
    KEY idx_quiz_generation_jobs_timeout_at (timeout_at),

    CONSTRAINT fk_quiz_generation_jobs_session_id
        FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions(id),
    CONSTRAINT chk_quiz_generation_jobs_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'timeout')),
    CONSTRAINT chk_quiz_generation_jobs_progress_pct
        CHECK (progress_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_quiz_generation_jobs_retryable
        CHECK (is_retryable IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 AI 생성 작업 이력';
