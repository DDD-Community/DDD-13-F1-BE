-- 퀴즈 문항
CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 문항 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    quiz_session_id BIGINT NOT NULL COMMENT '소속 퀴즈 세션 ID',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    subject_id BIGINT NOT NULL COMMENT '출제 과목 ID',
    chapter_id BIGINT NOT NULL COMMENT '출제 챕터 ID',
    part_id BIGINT NOT NULL COMMENT '출제 파트 ID',
    question_type VARCHAR(20) NOT NULL COMMENT '문항 유형',
    difficulty VARCHAR(10) NOT NULL COMMENT '문항 난이도',
    body TEXT NOT NULL COMMENT '문항 본문',
    summary VARCHAR(20) NULL COMMENT 'AI 생성 문항 한줄 요약',
    correct_explanation TEXT NULL COMMENT '정답해설',
    incorrect_explanation TEXT NULL COMMENT '오답해설',
    display_order TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '세션 내 문항 순서',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_questions_public_id (public_id),
    KEY idx_questions_quiz_session_id_order (quiz_session_id, display_order),
    KEY idx_questions_part_id (part_id),
    KEY idx_questions_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_questions_question_type
        CHECK (question_type IN ('multiple_choice', 'ox')),
    CONSTRAINT chk_questions_difficulty
        CHECK (difficulty IN ('easy', 'medium', 'hard')),
    CONSTRAINT chk_questions_summary
        CHECK (summary IS NULL OR CHAR_LENGTH(summary) BETWEEN 8 AND 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 생성 퀴즈 문항';

-- 퀴즈 문항 선택지
CREATE TABLE question_options (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    question_id BIGINT NOT NULL COMMENT '문항 ID',
    option_number TINYINT UNSIGNED NOT NULL COMMENT '보기 번호',
    content TEXT NOT NULL COMMENT '선택지 내용',
    is_correct TINYINT(1) NOT NULL DEFAULT 0 COMMENT '정답 여부',

    PRIMARY KEY (id),
    UNIQUE KEY uq_question_options_question_option (question_id, option_number),
    KEY idx_question_options_question_id (question_id),

    CONSTRAINT chk_question_options_is_correct
        CHECK (is_correct IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='객관식 문항 선택지';

-- 퀴즈 문항 정답
CREATE TABLE question_answers (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    question_id BIGINT NOT NULL COMMENT '문항 ID',
    answer_value VARCHAR(10) NOT NULL COMMENT '정답값',

    PRIMARY KEY (id),
    UNIQUE KEY uq_question_answers_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='문항 정답 키';
