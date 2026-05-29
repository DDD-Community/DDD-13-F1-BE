-- subject/chapter/lecture/part 도메인 스키마 보강

CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 내부 과목 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    name VARCHAR(30) NOT NULL COMMENT '과목명',
    purpose VARCHAR(20) NOT NULL COMMENT '학습목적: exam / review / other',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subjects_public_id (public_id),
    KEY idx_subjects_user_id_deleted_at (user_id, deleted_at),
    KEY idx_subjects_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_subjects_name_length CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 30),
    CONSTRAINT chk_subjects_purpose CHECK (purpose IN ('exam', 'review', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 기본 정보';

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
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

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
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

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
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_other_details_subject_id (subject_id),

    CONSTRAINT chk_subject_other_details_usage_purpose
        CHECK (usage_purpose IN ('work', 'personal', 'hobby', 'memory', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 기타 목적 상세 정보';

CREATE TABLE IF NOT EXISTS subject_exam_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    subject_id BIGINT NOT NULL COMMENT '과목 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    exam_name VARCHAR(100) NULL COMMENT '시험명',
    exam_date DATE NOT NULL COMMENT '시험 날짜',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_exam_schedules_public_id (public_id),
    UNIQUE KEY uq_subject_exam_schedules_subject_id (subject_id),
    KEY idx_subject_exam_schedules_user_id_exam_date (user_id, exam_date),
    KEY idx_subject_exam_schedules_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목별 시험 일정';

CREATE TABLE IF NOT EXISTS certificates (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    name VARCHAR(100) NOT NULL COMMENT '자격증명',
    is_featured TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자주 찾는 자격증 여부',
    display_order INT NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_certificates_name (name),
    KEY idx_certificates_is_featured_display_order (is_featured, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사전 정의 자격증 목록';

CREATE TABLE IF NOT EXISTS chapters (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 챕터 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    subject_id BIGINT NOT NULL COMMENT '소속 과목 ID',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    name VARCHAR(30) NOT NULL COMMENT '챕터명',
    display_order INT NOT NULL DEFAULT 0 COMMENT '챕터 노출 순서',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_chapters_public_id (public_id),
    KEY idx_chapters_subject_id_deleted_at (subject_id, deleted_at),
    KEY idx_chapters_subject_id_display_order (subject_id, display_order),
    KEY idx_chapters_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_chapters_name_length CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챕터';

CREATE TABLE IF NOT EXISTS lecture_uploads (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 업로드 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    chapter_id BIGINT NOT NULL COMMENT '소속 챕터 ID',
    user_id BIGINT NOT NULL COMMENT '업로드 사용자 ID',
    upload_type VARCHAR(10) NOT NULL COMMENT '업로드 방식: pdf / image / text',
    part_split_method VARCHAR(10) NOT NULL COMMENT '파트 분류 방식: auto / manual',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '처리 상태',
    raw_text MEDIUMTEXT NULL COMMENT '추출된 원본 텍스트',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_lecture_uploads_public_id (public_id),
    KEY idx_lecture_uploads_chapter_id_status (chapter_id, status),
    KEY idx_lecture_uploads_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_lecture_uploads_upload_type CHECK (upload_type IN ('pdf', 'image', 'text')),
    CONSTRAINT chk_lecture_uploads_part_split_method CHECK (part_split_method IN ('auto', 'manual')),
    CONSTRAINT chk_lecture_uploads_status CHECK (status IN ('pending', 'processing', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의 업로드 처리 정보';

CREATE TABLE IF NOT EXISTS lecture_upload_files (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    lecture_upload_id BIGINT NOT NULL COMMENT '업로드 ID',
    file_url VARCHAR(500) NOT NULL COMMENT '저장된 파일 URL',
    file_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    file_size BIGINT NOT NULL COMMENT '파일 크기',
    file_type VARCHAR(20) NOT NULL COMMENT '파일 MIME 타입',
    display_order INT NOT NULL DEFAULT 0 COMMENT '이미지 순서',
    ocr_status VARCHAR(20) NULL COMMENT 'OCR 상태',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    KEY idx_lecture_upload_files_lecture_upload_id_order (lecture_upload_id, display_order),

    CONSTRAINT chk_lecture_upload_files_ocr_status
        CHECK (ocr_status IN ('pending', 'success', 'failed') OR ocr_status IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의 업로드 파일 목록';

CREATE TABLE IF NOT EXISTS lecture_processing_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    lecture_upload_id BIGINT NOT NULL COMMENT '업로드 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '작업 상태',
    progress_pct TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '진행률',
    estimated_seconds SMALLINT NULL COMMENT '최초 예상 소요 시간',
    estimated_finish_at DATETIME(3) NULL COMMENT '예상 완료 시각',
    started_at DATETIME(3) NULL COMMENT '처리 시작 시각',
    completed_at DATETIME(3) NULL COMMENT '실제 완료 시각',
    timeout_at DATETIME(3) NULL COMMENT '타임아웃 기준 시각',
    retry_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '재시도 누적 횟수',
    is_retryable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '재시도 가능 여부',
    mq_message_id VARCHAR(100) NULL COMMENT 'RabbitMQ message-id',
    fail_code VARCHAR(50) NULL COMMENT '실패 코드',
    fail_reason VARCHAR(500) NULL COMMENT '실패 상세 사유',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_lecture_processing_jobs_upload_id (lecture_upload_id),
    KEY idx_lecture_processing_jobs_user_id_status (user_id, status),
    KEY idx_lecture_processing_jobs_status_timeout_at (status, timeout_at),

    CONSTRAINT chk_lecture_processing_jobs_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'timeout')),
    CONSTRAINT chk_lecture_processing_jobs_progress_pct
        CHECK (progress_pct BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의 업로드 처리 작업 이력';

CREATE TABLE IF NOT EXISTS parts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 파트 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    chapter_id BIGINT NOT NULL COMMENT '소속 챕터 ID',
    subject_id BIGINT NOT NULL COMMENT '소속 과목 ID',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    lecture_upload_id BIGINT NULL COMMENT '생성 원본 업로드 ID',
    name VARCHAR(100) NOT NULL COMMENT '파트명',
    part_number INT NOT NULL COMMENT '파트 번호',
    content MEDIUMTEXT NULL COMMENT '파트 본문',
    is_content_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '본문 삭제 여부',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_parts_public_id (public_id),
    KEY idx_parts_chapter_id_part_number (chapter_id, part_number),
    KEY idx_parts_chapter_id_deleted_at (chapter_id, deleted_at),
    KEY idx_parts_subject_id_deleted_at (subject_id, deleted_at),
    KEY idx_parts_user_id_created_at (user_id, created_at),
    KEY idx_parts_lecture_upload_id (lecture_upload_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트';

CREATE TABLE IF NOT EXISTS part_split_plans (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    lecture_upload_id BIGINT NOT NULL COMMENT '업로드 ID',
    part_number INT NOT NULL COMMENT '계획된 파트 번호',
    intended_name VARCHAR(100) NULL COMMENT '사용자 입력 파트명',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_part_split_plans_upload_part (lecture_upload_id, part_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='직접 분류 방식의 파트 계획 입력값';

SET @add_subject_exam_detail_deleted_at_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'subject_exam_details'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE subject_exam_details ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_subject_exam_detail_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_subject_review_detail_deleted_at_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'subject_review_details'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE subject_review_details ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_subject_review_detail_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_subject_other_detail_deleted_at_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'subject_other_details'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE subject_other_details ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_subject_other_detail_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_subject_exam_schedule_public_id_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'subject_exam_schedules'
              AND column_name = 'public_id'
        ),
        'ALTER TABLE subject_exam_schedules ADD COLUMN public_id CHAR(36) NULL COMMENT ''외부 노출용 UUID v7'' AFTER id',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_subject_exam_schedule_public_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE subject_exam_schedules
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE subject_exam_schedules
    MODIFY public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7';

SET @add_subject_exam_schedule_public_id_index_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'subject_exam_schedules'
              AND index_name = 'uq_subject_exam_schedules_public_id'
        ),
        'ALTER TABLE subject_exam_schedules ADD UNIQUE KEY uq_subject_exam_schedules_public_id (public_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_subject_exam_schedule_public_id_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_quiz_result_public_id_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_results'
              AND column_name = 'public_id'
        ),
        'ALTER TABLE quiz_results ADD COLUMN public_id CHAR(36) NULL COMMENT ''외부 노출용 UUID v7'' AFTER id',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_quiz_result_public_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE quiz_results
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE quiz_results
    MODIFY public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7';

SET @add_quiz_result_public_id_index_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_results'
              AND index_name = 'uq_quiz_results_public_id'
        ),
        'ALTER TABLE quiz_results ADD UNIQUE KEY uq_quiz_results_public_id (public_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_quiz_result_public_id_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_quiz_result_deleted_at_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_results'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE quiz_results ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_quiz_result_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
