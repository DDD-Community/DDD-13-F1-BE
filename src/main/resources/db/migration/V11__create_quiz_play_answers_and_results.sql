CREATE TABLE IF NOT EXISTS quiz_play_answers (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    play_session_id BIGINT NOT NULL COMMENT '풀이 세션 ID',
    question_id BIGINT NOT NULL COMMENT '문항 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    selected_option_id BIGINT NULL COMMENT '선택한 선택지 ID',
    selected_value VARCHAR(10) NULL COMMENT '선택한 값',
    is_correct_client TINYINT(1) NULL COMMENT '클라이언트 채점 결과',
    is_correct_server TINYINT(1) NULL COMMENT '서버 재채점 결과',
    is_skipped TINYINT(1) NOT NULL DEFAULT 0 COMMENT '미선택 제출 여부',
    answer_elapsed_ms INT UNSIGNED NULL COMMENT '문항 응답 소요 시간',
    is_marked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '어려움 마킹 여부',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_play_answers_session_question (play_session_id, question_id),
    KEY idx_quiz_play_answers_question_id (question_id),
    KEY idx_quiz_play_answers_user_id (user_id),

    CONSTRAINT chk_quiz_play_answers_is_correct_client
        CHECK (is_correct_client IN (0, 1) OR is_correct_client IS NULL),
    CONSTRAINT chk_quiz_play_answers_is_correct_server
        CHECK (is_correct_server IN (0, 1) OR is_correct_server IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='문항별 답안 및 채점 결과';

CREATE TABLE IF NOT EXISTS quiz_results (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    play_session_id BIGINT NOT NULL COMMENT '풀이 세션 ID',
    quiz_session_id BIGINT NOT NULL COMMENT '퀴즈 세션 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    total_count TINYINT UNSIGNED NOT NULL COMMENT '총 문항 수',
    correct_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '정답 수',
    wrong_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '오답 수',
    skip_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '미선택 수',
    accuracy_pct TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '정답률',
    elapsed_ms INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '누적 풀이 시간',
    dotori_earned SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '획득 도토리',
    xp_earned SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '획득 XP',
    is_leveled_up TINYINT(1) NOT NULL DEFAULT 0 COMMENT '레벨업 여부',
    new_level TINYINT UNSIGNED NULL COMMENT '새 레벨',
    is_score_matched TINYINT(1) NOT NULL DEFAULT 1 COMMENT '클라이언트-서버 채점 일치 여부',
    is_abuse_flagged TINYINT(1) NOT NULL DEFAULT 0 COMMENT '어뷰징 의심 여부',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '결과 저장 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_results_public_id (public_id),
    UNIQUE KEY uq_quiz_results_play_session_id (play_session_id),
    KEY idx_quiz_results_user_id_created_at (user_id, created_at),
    KEY idx_quiz_results_quiz_session_id (quiz_session_id),
    KEY idx_quiz_results_subject_id_created_at (subject_id, created_at),
    KEY idx_quiz_results_is_abuse_flagged (is_abuse_flagged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 풀이 결과 요약';
