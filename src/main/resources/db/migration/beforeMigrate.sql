-- V4/V13 선행 ALTER 대상 최소 스키마 보장
-- 기존 적용 마이그레이션 체크섬 보존용 Flyway callback

CREATE TABLE IF NOT EXISTS subject_exam_details (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    exam_type VARCHAR(20) NOT NULL COMMENT '시험유형: university / middle_high / certificate / civil_service / language / other_exam',
    univ_major_field VARCHAR(30) NULL COMMENT '전공 계열',
    univ_major_name VARCHAR(30) NULL COMMENT '전공명',
    univ_course_type VARCHAR(20) NULL COMMENT '과목 유형',
    mh_grade VARCHAR(10) NULL COMMENT '학년',
    mh_subject_type VARCHAR(30) NULL COMMENT '과목 유형',
    certificate_id BIGINT NULL COMMENT '자격증 목록 FK',
    certificate_name VARCHAR(100) NULL COMMENT '직접입력 자격증명',
    civil_rank VARCHAR(20) NULL COMMENT '급수',
    civil_series VARCHAR(30) NULL COMMENT '직렬',
    lang_type VARCHAR(20) NULL COMMENT '언어',
    lang_exam_name VARCHAR(30) NULL COMMENT '시험 상세명',
    other_exam_name VARCHAR(30) NULL COMMENT '기타 시험명',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_exam_details_subject_id (subject_id),
    KEY idx_subject_exam_details_exam_type (exam_type),

    CONSTRAINT chk_subject_exam_details_exam_type
        CHECK (exam_type IN ('university', 'middle_high', 'certificate', 'civil_service', 'language', 'other_exam'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 시험 상세 정보';

CREATE TABLE IF NOT EXISTS subject_review_details (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    field VARCHAR(30) NOT NULL COMMENT '분야',
    study_level VARCHAR(30) NOT NULL COMMENT '학습 정도',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_review_details_subject_id (subject_id),

    CONSTRAINT chk_subject_review_details_study_level
        CHECK (study_level IN ('beginner', 'casual', 'regular', 'expert'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 일반 복습 상세 정보';

CREATE TABLE IF NOT EXISTS subject_other_details (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    usage_purpose VARCHAR(30) NOT NULL COMMENT '이용목적: work / personal / hobby / memory / other',
    description VARCHAR(100) NULL COMMENT '추가 설명',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_other_details_subject_id (subject_id),

    CONSTRAINT chk_subject_other_details_usage_purpose
        CHECK (usage_purpose IN ('work', 'personal', 'hobby', 'memory', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 기타 목적 상세 정보';

CREATE TABLE IF NOT EXISTS subject_exam_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    exam_name VARCHAR(100) NULL COMMENT '시험명',
    exam_date DATE NOT NULL COMMENT '시험 날짜',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_exam_schedules_subject_id (subject_id),
    KEY idx_subject_exam_schedules_user_id_exam_date (user_id, exam_date),
    KEY idx_subject_exam_schedules_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목별 시험 일정';

CREATE TABLE IF NOT EXISTS quiz_results (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
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
    UNIQUE KEY uq_quiz_results_play_session_id (play_session_id),
    KEY idx_quiz_results_user_id_created_at (user_id, created_at),
    KEY idx_quiz_results_quiz_session_id (quiz_session_id),
    KEY idx_quiz_results_subject_id_created_at (subject_id, created_at),
    KEY idx_quiz_results_is_abuse_flagged (is_abuse_flagged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 풀이 결과 요약';
