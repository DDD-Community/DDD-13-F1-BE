-- 퀴즈 세션 (QUIZ-OPT-002/004/005/005B/006 + QUIZ-GEN-001 설정값/생성 상태)
CREATE TABLE quiz_sessions (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 퀴즈 세션 식별자',
    public_id           CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    user_id             BIGINT NOT NULL COMMENT '사용자 ID',
    subject_id          BIGINT NOT NULL COMMENT '출제 과목 ID (역정규화)',

    quiz_type           VARCHAR(20) NOT NULL COMMENT '퀴즈 형식: multiple_choice / ox',
    choice_count        TINYINT NULL COMMENT '객관식 보기 수: 4 또는 5 (OX이면 NULL)',
    question_count      TINYINT UNSIGNED NOT NULL COMMENT '요청 문제 수 (1~100)',
    play_mode           VARCHAR(20) NOT NULL COMMENT '풀기 방식: one_by_one / all_at_once',

    timer_enabled       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '타이머 사용 여부',
    timer_scope         VARCHAR(20) NULL COMMENT '타이머 범위: per_question / total',
    timer_seconds       INT NULL COMMENT '타이머 시간(초 단위 통일 저장)',

    difficulty          VARCHAR(10) NOT NULL DEFAULT 'medium' COMMENT '난이도: easy / medium / hard',

    status              VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '생성 상태: pending / in_progress / completed / failed',
    job_id              VARCHAR(100) NULL COMMENT '비동기 작업 ID',
    fail_reason         VARCHAR(255) NULL COMMENT '실패 사유 (status=failed)',
    generated_count     TINYINT UNSIGNED NULL COMMENT '실제 생성된 문제 수',
    completed_at        DATETIME(3) NULL COMMENT '생성 완료 시각',

    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at          DATETIME(3) NULL COMMENT '삭제 시각 (soft delete)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_sessions_public_id (public_id),
    KEY idx_quiz_sessions_user_id_status (user_id, status),
    KEY idx_quiz_sessions_user_id_created_at (user_id, created_at),
    KEY idx_quiz_sessions_subject_id_created_at (subject_id, created_at),
    KEY idx_quiz_sessions_status_created_at (status, created_at),

    CONSTRAINT chk_quiz_sessions_quiz_type
        CHECK (quiz_type IN ('multiple_choice', 'ox')),
    CONSTRAINT chk_quiz_sessions_choice_count
        CHECK (choice_count IS NULL OR choice_count IN (4, 5)),
    CONSTRAINT chk_quiz_sessions_question_count
        CHECK (question_count BETWEEN 1 AND 100),
    CONSTRAINT chk_quiz_sessions_play_mode
        CHECK (play_mode IN ('one_by_one', 'all_at_once')),
    CONSTRAINT chk_quiz_sessions_timer_scope
        CHECK (timer_scope IS NULL OR timer_scope IN ('per_question', 'total')),
    CONSTRAINT chk_quiz_sessions_difficulty
        CHECK (difficulty IN ('easy', 'medium', 'hard')),
    CONSTRAINT chk_quiz_sessions_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 세션 설정값 및 생성 상태';

-- 퀴즈 세션 출제 범위 (세션당 N개 파트)
CREATE TABLE quiz_session_scopes (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    quiz_session_id     BIGINT NOT NULL COMMENT '퀴즈 세션 ID',
    part_id             BIGINT NOT NULL COMMENT '출제 대상 파트 ID',
    chapter_id          BIGINT NOT NULL COMMENT '소속 챕터 ID (역정규화)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_session_scopes_session_part (quiz_session_id, part_id),
    KEY idx_quiz_session_scopes_quiz_session_id (quiz_session_id),
    KEY idx_quiz_session_scopes_part_id (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 세션 출제 범위 (파트 단위)';
