CREATE TABLE quiz_play_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    client_session_id VARCHAR(36) NOT NULL COMMENT '클라이언트 발급 풀이 세션 ID',
    quiz_session_id BIGINT NOT NULL COMMENT '퀴즈 세션 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    play_type VARCHAR(20) NOT NULL DEFAULT 'first' COMMENT '풀이 유형',
    parent_play_session_id BIGINT NULL COMMENT '부모 풀이 세션 ID',
    parent_quiz_session_id BIGINT NULL COMMENT '부모 퀴즈 세션 ID',
    generation TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '오답 복습 세대',
    is_question_shuffled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '문제 순서 셔플 여부',
    is_option_shuffled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '선택지 순서 셔플 여부',
    shuffle_seed VARCHAR(100) NULL COMMENT '셔플 시드값',
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT '풀이 상태',
    last_question_index TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '마지막 문항 인덱스',
    elapsed_ms INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '누적 풀이 시간',
    submitted_at DATETIME(3) NULL COMMENT '결과 제출 시각',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_play_sessions_client_session_id (client_session_id),
    KEY idx_quiz_play_sessions_quiz_session_id (quiz_session_id),
    KEY idx_quiz_play_sessions_user_id_status (user_id, status),
    KEY idx_quiz_play_sessions_user_id_created_at (user_id, created_at),
    KEY idx_quiz_play_sessions_parent_play_session_id (parent_play_session_id),
    KEY idx_quiz_play_sessions_subject_id_created_at (subject_id, created_at),

    CONSTRAINT fk_quiz_play_sessions_quiz_session_id
        FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions(id),
    CONSTRAINT chk_quiz_play_sessions_play_type
        CHECK (play_type IN ('first', 'retry_all', 'retry_wrong')),
    CONSTRAINT chk_quiz_play_sessions_status
        CHECK (status IN ('in_progress', 'submitted', 'abandoned')),
    CONSTRAINT chk_quiz_play_sessions_question_shuffled
        CHECK (is_question_shuffled IN (0, 1)),
    CONSTRAINT chk_quiz_play_sessions_option_shuffled
        CHECK (is_option_shuffled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 풀이 세션';
