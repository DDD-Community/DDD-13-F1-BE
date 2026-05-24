-- ============================================================
-- 사용자/인증 관련 테이블
-- ============================================================
  
-- 사용자 기본 정보
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK: 내부 사용자 식별자',
    public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    email VARCHAR(255) NOT NULL COMMENT '사용자 이메일',
    nickname VARCHAR(36) NOT NULL COMMENT '사용자 닉네임',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '계정 상태',
    is_email_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '이메일 인증 여부',
    failed_login_count INT NOT NULL DEFAULT 0 COMMENT '로그인 실패 횟수',
    last_failed_login_at DATETIME(3) NULL COMMENT '마지막 로그인 실패 시각',
    locked_at DATETIME(3) NULL COMMENT '계정 잠금 시각',
    last_login_at DATETIME(3) NULL COMMENT '마지막 로그인 시각',
    last_login_ip VARCHAR(45) NULL COMMENT '마지막 로그인 IP',
    dotori_balance INT NOT NULL DEFAULT 0 COMMENT '도토리 잔고',
    xp_total            INT UNSIGNED     NOT NULL DEFAULT 0  COMMENT '누적 XP 총량 (캐시값, 원장은 user_xp_logs)',
    current_level       TINYINT UNSIGNED NOT NULL DEFAULT 1  COMMENT '현재 레벨 1~10 (캐시값, 원장은 user_xp_logs)',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),  
    UNIQUE KEY uq_users_public_id (public_id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_status_created_at (status, created_at),
    KEY idx_users_deleted_at_created_at (deleted_at, created_at),

    CONSTRAINT chk_users_status CHECK (status IN ('active', 'locked', 'inactive')),
    CONSTRAINT chk_users_is_email_verified CHECK (is_email_verified IN (0, 1)),
    CONSTRAINT chk_users_failed_login_count CHECK (failed_login_count >= 0),
    CONSTRAINT chk_users_dotori_balance CHECK (dotori_balance >= 0),
    CONSTRAINT chk_users_xp_total CHECK (xp_total >= 0),
    CONSTRAINT chk_users_current_level CHECK (current_level BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 기본 정보';

-- 사용자 인증 수단
CREATE TABLE user_auth_identities (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '로그인 수단 레코드 식별 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    provider VARCHAR(20) NOT NULL COMMENT '로그인 수단 구분값',
    provider_subject VARCHAR(255) NOT NULL DEFAULT '' COMMENT '로그인 수단별 사용자 고유 식별값, local은 users.public_id',
    password_hash VARCHAR(255) NULL COMMENT '자체 로그인 비밀번호 해시값',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 로그인 수단 여부',
    last_login_at DATETIME(3) NULL COMMENT '해당 인증 수단 마지막 로그인 시각',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_auth_identities_provider_subject (provider, provider_subject),
    UNIQUE KEY uq_user_auth_identities_user_id_provider (user_id, provider),
    KEY idx_user_auth_identities_user_id_created_at (user_id, created_at),
    KEY idx_user_auth_identities_provider_created_at (provider, created_at),

    -- CONSTRAINT fk_user_auth_identities_user_id
    --     FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_auth_identities_provider
        CHECK (provider IN ('local', 'kakao', 'apple')),
    CONSTRAINT chk_user_auth_identities_is_primary
        CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 인증 수단';

-- 이메일 인증 요청 및 처리 이력
CREATE TABLE user_email_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이메일 인증 요청 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    email VARCHAR(255) NOT NULL COMMENT '인증 대상 이메일',
    verification_token VARCHAR(255) NOT NULL COMMENT '이메일 인증 토큰',
    verification_code VARCHAR(20) NULL COMMENT '사용자 입력용 인증 코드',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '인증 상태',
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '인증 요청 시각',
    verified_at DATETIME(3) NULL COMMENT '인증 완료 시각',
    expires_at DATETIME(3) NOT NULL COMMENT '만료 시각',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email_verifications_verification_token (verification_token),
    KEY idx_user_email_verifications_user_id_status (user_id, status),
    KEY idx_user_email_verifications_email_status (email, status),
    KEY idx_user_email_verifications_expires_at (expires_at),

    -- CONSTRAINT fk_user_email_verifications_user_id
    --     FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_email_verifications_status
        CHECK (status IN ('pending', 'verified', 'expired', 'cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이메일 인증 요청 및 처리 이력';

-- 비밀번호 재설정 토큰 관리
CREATE TABLE user_password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '비밀번호 재설정 토큰 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    reset_token VARCHAR(255) NOT NULL COMMENT '재설정 토큰',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '토큰 상태',
    requested_ip VARCHAR(45) NULL COMMENT '요청 IP',
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '요청 시각',
    used_at DATETIME(3) NULL COMMENT '사용 완료 시각',
    expires_at DATETIME(3) NOT NULL COMMENT '만료 시각',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_password_reset_tokens_reset_token (reset_token),
    KEY idx_user_password_reset_tokens_user_id_status (user_id, status),
    KEY idx_user_password_reset_tokens_expires_at (expires_at),

    -- CONSTRAINT fk_user_password_reset_tokens_user_id
    --     FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_password_reset_tokens_status
        CHECK (status IN ('pending', 'used', 'expired', 'cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='비밀번호 재설정 토큰 관리';

-- 사용자 리프레시 토큰 관리
CREATE TABLE user_refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '리프레시 토큰 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    token_hash VARCHAR(255) NOT NULL COMMENT '리프레시 토큰 해시값',
    device_id VARCHAR(128) NULL COMMENT '디바이스 식별자',
    device_name VARCHAR(100) NULL COMMENT '디바이스 이름',
    user_agent VARCHAR(500) NULL COMMENT '사용자 에이전트',
    ip_address VARCHAR(45) NULL COMMENT 'IP 주소',
    issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '발급 시각',
    expires_at DATETIME(3) NOT NULL COMMENT '만료 시각',
    revoked_at DATETIME(3) NULL COMMENT '폐기 시각',
    last_used_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '마지막 사용 시각',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at DATETIME(3) NULL COMMENT '삭제 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_refresh_tokens_token_hash (token_hash),
    KEY idx_user_refresh_tokens_user_id_expires_at (user_id, expires_at),
    KEY idx_user_refresh_tokens_user_id_revoked_at (user_id, revoked_at),
    KEY idx_user_refresh_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 리프레시 토큰 관리';

-- ============================================================
-- SUBJECT (과목) 관련 테이블
-- ============================================================

-- 과목 기본 정보
CREATE TABLE subjects (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 내부 과목 식별자',
    public_id           CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    user_id             BIGINT          NOT NULL                         COMMENT '소유 사용자 ID',
    name                VARCHAR(30)     NOT NULL                         COMMENT '과목명 (1~30자)',
    purpose             VARCHAR(20)     NOT NULL                         COMMENT '학습목적: exam / review / other',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at          DATETIME(3)     NULL                             COMMENT '삭제 시각 (soft delete)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subjects_public_id (public_id),
    KEY idx_subjects_user_id_deleted_at (user_id, deleted_at),
    KEY idx_subjects_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_subjects_name_length CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 30),
    CONSTRAINT chk_subjects_purpose CHECK (purpose IN ('exam', 'review', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 기본 정보';


-- 과목 상세 - 시험/자격증 대비 (purpose = exam)
-- exam_type별 하위 필드를 하나의 테이블에 nullable 컬럼으로 관리
-- (step3 입력 필드는 exam_type에 따라 달라지므로 범용 컬럼 + JSON 보조)
CREATE TABLE subject_exam_details (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    subject_id          BIGINT          NOT NULL                         COMMENT '과목 ID',
    exam_type           VARCHAR(20)     NOT NULL                         COMMENT '시험유형: university / middle_high / certificate / civil_service / language / other_exam',

    -- #대학 시험
    univ_major_field    VARCHAR(30)     NULL                             COMMENT '전공 계열 (인문/사회, 자연/과학 등)',
    univ_major_name     VARCHAR(30)     NULL                             COMMENT '전공명 (1~30자)',
    univ_course_type    VARCHAR(20)     NULL                             COMMENT '과목 유형: major / liberal_arts',

    -- #중·고등 시험
    mh_grade            VARCHAR(10)     NULL                             COMMENT '학년: 중1~고3',
    mh_subject_type     VARCHAR(30)     NULL                             COMMENT '과목 유형: 국어/수학/영어 등, 직접입력 포함',

    -- #자격증
    certificate_id      BIGINT          NULL                             COMMENT '자격증 목록 FK (certificates.id), NULL이면 직접입력',
    certificate_name    VARCHAR(100)    NULL                             COMMENT '직접입력 자격증명 (해당 유저·과목에만 종속)',

    -- #공무원
    civil_rank          VARCHAR(20)     NULL                             COMMENT '급수: 9급/7급/5급/경찰직/소방직/기타 특수직',
    civil_series        VARCHAR(30)     NULL                             COMMENT '직렬 (경찰직·소방직·기타 특수직은 NULL)',

    -- #어학
    lang_type           VARCHAR(20)     NULL                             COMMENT '언어: english / japanese / chinese',
    lang_exam_name      VARCHAR(30)     NULL                             COMMENT '시험 상세명 (TOEIC, JLPT 등, 직접입력 포함)',

    -- #기타시험
    other_exam_name     VARCHAR(30)     NULL                             COMMENT '기타 시험명 (1~30자)',

    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_exam_details_subject_id (subject_id),
    KEY idx_subject_exam_details_exam_type (exam_type),

    CONSTRAINT chk_subject_exam_details_exam_type
        CHECK (exam_type IN ('university', 'middle_high', 'certificate', 'civil_service', 'language', 'other_exam'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 시험 상세 정보 (purpose=exam)';


-- 과목 상세 - 일반 복습/자기계발 (purpose = review)
CREATE TABLE subject_review_details (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    subject_id          BIGINT          NOT NULL                         COMMENT '과목 ID',
    field               VARCHAR(30)     NOT NULL                         COMMENT '분야 (인문/한국사/IT 등, 직접입력 포함)',
    study_level         VARCHAR(30)     NOT NULL                         COMMENT '학습 정도 (처음 공부해봐요 등 4단계)',

    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_review_details_subject_id (subject_id),

    CONSTRAINT chk_subject_review_details_study_level
        CHECK (study_level IN ('beginner', 'casual', 'regular', 'expert'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 일반 복습 상세 정보 (purpose=review)';


-- 과목 상세 - 기타 목적 (purpose = other)
CREATE TABLE subject_other_details (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    subject_id          BIGINT          NOT NULL                         COMMENT '과목 ID',
    usage_purpose       VARCHAR(30)     NOT NULL                         COMMENT '이용목적: work / personal / hobby / memory / other',
    description         VARCHAR(100)    NULL                             COMMENT '추가 설명 (1~100자)',

    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_other_details_subject_id (subject_id),

    CONSTRAINT chk_subject_other_details_usage_purpose
        CHECK (usage_purpose IN ('work', 'personal', 'hobby', 'memory', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목 기타 목적 상세 정보 (purpose=other)';


-- 시험 일정 (D-Day) - MVP: 과목당 1개
CREATE TABLE subject_exam_schedules (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    public_id           CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    subject_id          BIGINT          NOT NULL                         COMMENT '과목 ID',
    user_id             BIGINT          NOT NULL                         COMMENT '사용자 ID (조회 성능용 역정규화)',
    exam_name           VARCHAR(100)    NULL                             COMMENT '시험명 (미입력 시 과목명 사용)',
    exam_date           DATE            NOT NULL                         COMMENT '시험 날짜',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at          DATETIME(3)     NULL                             COMMENT '삭제 시각 (soft delete)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_exam_schedules_public_id (public_id),
    UNIQUE KEY uq_subject_exam_schedules_subject_id (subject_id),  -- MVP: 과목당 1개
    KEY idx_subject_exam_schedules_user_id_exam_date (user_id, exam_date),
    KEY idx_subject_exam_schedules_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='과목별 시험 일정 (D-Day)';


-- 사전 정의 자격증 목록 (DB 사전 저장)
CREATE TABLE certificates (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    name                VARCHAR(100)    NOT NULL                         COMMENT '자격증명',
    is_featured         TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '자주 찾는 자격증 여부 (MVP: 수동 지정)',
    display_order       INT             NOT NULL DEFAULT 0               COMMENT '노출 순서',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_certificates_name (name),
    KEY idx_certificates_is_featured_display_order (is_featured, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사전 정의 자격증 목록';


-- ============================================================
-- CHAPTER (챕터) 관련 테이블
-- ============================================================

-- 챕터 (강의 묶음 단위)
CREATE TABLE chapters (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 챕터 식별자',
    public_id           CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    subject_id          BIGINT          NOT NULL                         COMMENT '소속 과목 ID',
    user_id             BIGINT          NOT NULL                         COMMENT '소유 사용자 ID (조회 성능용 역정규화)',
    name                VARCHAR(30)     NOT NULL                         COMMENT '챕터명 (1~30자)',
    display_order       INT             NOT NULL DEFAULT 0               COMMENT '챕터 노출 순서',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at          DATETIME(3)     NULL                             COMMENT '삭제 시각 (soft delete)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_chapters_public_id (public_id),
    KEY idx_chapters_subject_id_deleted_at (subject_id, deleted_at),
    KEY idx_chapters_subject_id_display_order (subject_id, display_order),
    KEY idx_chapters_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_chapters_name_length CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챕터 (강의 묶음 단위)';


-- ============================================================
-- LECTURE (강의 업로드) 관련 테이블
-- ============================================================

-- 강의 업로드 원본 파일 정보
-- PDF / 이미지 / 텍스트 업로드 모두 포함
CREATE TABLE lecture_uploads (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 업로드 식별자',
    public_id           CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    chapter_id          BIGINT          NOT NULL                         COMMENT '소속 챕터 ID',
    user_id             BIGINT          NOT NULL                         COMMENT '업로드 사용자 ID',
    upload_type         VARCHAR(10)     NOT NULL                         COMMENT '업로드 방식: pdf / image / text',
    part_split_method   VARCHAR(10)     NOT NULL                         COMMENT '파트 분류 방식: auto / manual',
    status              VARCHAR(20)     NOT NULL DEFAULT 'pending'       COMMENT '처리 상태: pending / processing / completed / failed',
    raw_text            MEDIUMTEXT      NULL                             COMMENT '추출된 원본 텍스트 (텍스트 직접입력 or OCR 결과)',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_lecture_uploads_public_id (public_id),
    KEY idx_lecture_uploads_chapter_id_status (chapter_id, status),
    KEY idx_lecture_uploads_user_id_created_at (user_id, created_at),

    CONSTRAINT chk_lecture_uploads_upload_type CHECK (upload_type IN ('pdf', 'image', 'text')),
    CONSTRAINT chk_lecture_uploads_part_split_method CHECK (part_split_method IN ('auto', 'manual')),
    CONSTRAINT chk_lecture_uploads_status CHECK (status IN ('pending', 'processing', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의 업로드 처리 정보';


-- 업로드 파일 메타 (PDF or 이미지 파일별 정보)
-- 이미지는 여러 장이므로 별도 테이블로 분리
CREATE TABLE lecture_upload_files (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    lecture_upload_id   BIGINT          NOT NULL                         COMMENT '업로드 ID',
    file_url            VARCHAR(500)    NOT NULL                         COMMENT '저장된 파일 URL (S3 등)',
    file_name           VARCHAR(255)    NOT NULL                         COMMENT '원본 파일명',
    file_size           BIGINT          NOT NULL                         COMMENT '파일 크기 (bytes)',
    file_type           VARCHAR(20)     NOT NULL                         COMMENT '파일 MIME 타입',
    display_order       INT             NOT NULL DEFAULT 0               COMMENT '이미지 순서 (이미지 업로드 시 사용자 지정 순서)',
    ocr_status          VARCHAR(20)     NULL                             COMMENT 'OCR 상태 (이미지 업로드 시): pending / success / failed',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    KEY idx_lecture_upload_files_lecture_upload_id_order (lecture_upload_id, display_order),

    CONSTRAINT chk_lecture_upload_files_ocr_status
        CHECK (ocr_status IN ('pending', 'success', 'failed') OR ocr_status IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의 업로드 파일 목록 (PDF/이미지)';

CREATE TABLE lecture_processing_jobs (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    lecture_upload_id   BIGINT          NOT NULL                         COMMENT '업로드 ID',
    user_id             BIGINT          NOT NULL                         COMMENT '사용자 ID (역정규화)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'pending'       COMMENT '작업 상태: pending / in_progress / completed / failed / timeout',
    progress_pct        TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '진행률 0~100',
    estimated_seconds   SMALLINT        NULL                             COMMENT '최초 예상 소요 시간(초)',
    estimated_finish_at DATETIME(3)     NULL                             COMMENT '예상 완료 시각 = started_at + estimated_seconds',
    started_at          DATETIME(3)     NULL                             COMMENT '처리 시작 시각',
    completed_at        DATETIME(3)     NULL                             COMMENT '실제 완료 시각',
    timeout_at          DATETIME(3)     NULL                             COMMENT '타임아웃 기준 시각 (= started_at + 600초)',
    retry_count         TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '재시도 누적 횟수',
    is_retryable        TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '재시도 가능 여부',
    mq_message_id       VARCHAR(100)    NULL                             COMMENT 'RabbitMQ message-id (MVP는 NULL)',
    fail_code           VARCHAR(50)     NULL                             COMMENT '실패 코드',
    fail_reason         VARCHAR(500)    NULL                             COMMENT '실패 상세 사유',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_lecture_processing_jobs_upload_id (lecture_upload_id), -- 업로드당 1개 보장
    KEY idx_lecture_processing_jobs_user_id_status (user_id, status),
    KEY idx_lecture_processing_jobs_status_timeout_at (status, timeout_at),

    CONSTRAINT chk_lecture_processing_jobs_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'timeout')),
    CONSTRAINT chk_lecture_processing_jobs_progress_pct
        CHECK (progress_pct BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='강의 업로드 OCR/파트 분류 처리 작업 이력 (추후 도입 시 MQ 도입시 사용, MVP 구현 단계에서는 미사용)';

-- ============================================================
-- PART (파트) 관련 테이블
-- ============================================================

-- 파트 (챕터 하위 내용 단위)
CREATE TABLE parts (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 파트 식별자',
    public_id           CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    chapter_id          BIGINT          NOT NULL                         COMMENT '소속 챕터 ID',
    subject_id          BIGINT          NOT NULL                         COMMENT '소속 과목 ID (조회 성능용 역정규화)',
    user_id             BIGINT          NOT NULL                         COMMENT '소유 사용자 ID (조회 성능용 역정규화)',
    lecture_upload_id   BIGINT          NULL                             COMMENT '생성 원본 업로드 ID (NULL이면 파트 직접 추가)',
    name                VARCHAR(100)    NOT NULL                         COMMENT '파트명 (미입력 시 AI 자동 생성)',
    part_number         INT             NOT NULL                         COMMENT '파트 번호 (챕터 내 순서, 삭제 시 재정렬)',
    content             MEDIUMTEXT      NULL                             COMMENT '파트 본문 (최대 30,000자, 삭제 시 NULL)',
    is_content_deleted  TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '본문 삭제 여부 (파트 삭제 시 content만 제거, 레코드는 유지)',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at          DATETIME(3)     NULL                             COMMENT '삭제 시각 (soft delete)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_parts_public_id (public_id),
    KEY idx_parts_chapter_id_part_number (chapter_id, part_number),
    KEY idx_parts_chapter_id_deleted_at (chapter_id, deleted_at),
    KEY idx_parts_subject_id_deleted_at (subject_id, deleted_at),
    KEY idx_parts_user_id_created_at (user_id, created_at),
    KEY idx_parts_lecture_upload_id (lecture_upload_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트 (챕터 하위 강의 내용 단위)';


-- 파트 분류 입력값 (직접 분류 방식일 때 사용자가 입력한 파트명 계획)
-- 업로드 전 사용자가 설정한 파트 구성 의도를 저장 (AI 분류 참고용)
CREATE TABLE part_split_plans (
    id                  BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    lecture_upload_id   BIGINT          NOT NULL                         COMMENT '업로드 ID',
    part_number         INT             NOT NULL                         COMMENT '계획된 파트 번호',
    intended_name       VARCHAR(100)    NULL                             COMMENT '사용자 입력 파트명 (NULL이면 AI 자동 생성)',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_part_split_plans_upload_part (lecture_upload_id, part_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='직접 분류 방식의 파트 계획 입력값';

-- ============================================================
-- QUIZ SESSION (퀴즈 세션) — 설정값 묶음
-- ============================================================

-- 퀴즈 세션: 사용자가 설정한 한 번의 퀴즈 요청 단위
-- 범위·형식·문제수·풀기방식·타이머·난이도를 하나의 레코드로 관리
CREATE TABLE quiz_sessions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 퀴즈 세션 식별자',
    public_id               CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID',
    subject_id              BIGINT          NOT NULL                         COMMENT '출제 과목 ID (역정규화 — 빠른 필터)',

    -- 퀴즈 형식 (QUIZ-OPT-002)
    quiz_type               VARCHAR(20)     NOT NULL                         COMMENT '퀴즈 형식: multiple_choice / ox',
    choice_count            TINYINT         NULL                             COMMENT '객관식 보기 수: 4 또는 5 (OX이면 NULL)',

    -- 문제 수 (QUIZ-OPT-004)
    question_count          TINYINT UNSIGNED NOT NULL                        COMMENT '요청 문제 수 (1~100)',

    -- 풀기 방식 (QUIZ-OPT-005)
    play_mode               VARCHAR(20)     NOT NULL                         COMMENT '풀기 방식: one_by_one / all_at_once',

    -- 타이머 (QUIZ-OPT-005B)
    timer_enabled           TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '타이머 사용 여부',
    timer_scope             VARCHAR(20)     NULL                             COMMENT '타이머 범위: per_question / total (timer_enabled=1일 때만)',
    timer_seconds           INT             NULL                             COMMENT '타이머 시간(초 단위 통일 저장, NULL이면 미사용)',

    -- 난이도 (QUIZ-OPT-006)
    difficulty              VARCHAR(10)     NOT NULL DEFAULT 'medium'        COMMENT '난이도: easy / medium / hard',

    -- 생성 상태 (QUIZ-GEN-001)
    status                  VARCHAR(20)     NOT NULL DEFAULT 'pending'       COMMENT '생성 상태: pending / in_progress / completed / failed',
    job_id                  VARCHAR(100)    NULL                             COMMENT '비동기 작업 ID (백그라운드 생성 추적)',
    fail_reason             VARCHAR(255)    NULL                             COMMENT '실패 사유 (status=failed일 때)',
    generated_count         TINYINT UNSIGNED NULL                           COMMENT '실제 생성된 문제 수 (텍스트 부족 시 요청보다 적을 수 있음)',
    completed_at            DATETIME(3)     NULL                             COMMENT '생성 완료 시각',

    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    deleted_at              DATETIME(3)     NULL                             COMMENT '삭제 시각 (soft delete)',

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 세션 (설정값 + 생성 상태)';


-- ============================================================
-- QUIZ SCOPE (퀴즈 출제 범위) — 세션당 N개 파트
-- ============================================================

-- 퀴즈 범위: 세션에 선택된 파트 목록
-- 챕터 선택 시 서버가 하위 part_id로 펼쳐서 저장
CREATE TABLE quiz_session_scopes (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    quiz_session_id         BIGINT          NOT NULL                         COMMENT '퀴즈 세션 ID',
    part_id                 BIGINT          NOT NULL                         COMMENT '출제 대상 파트 ID',
    chapter_id              BIGINT          NOT NULL                         COMMENT '소속 챕터 ID (역정규화 — 범위 표시용)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_session_scopes_session_part (quiz_session_id, part_id),
    KEY idx_quiz_session_scopes_quiz_session_id (quiz_session_id),
    KEY idx_quiz_session_scopes_part_id (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 세션 출제 범위 (파트 단위)';


-- ============================================================
-- QUESTIONS (문항) — AI 생성 결과
-- ============================================================

-- 문항: AI가 생성한 개별 퀴즈 문항
-- subject_id, chapter_id, part_id 비정규화 저장 (명세 명시)
CREATE TABLE questions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 문항 식별자',
    public_id               CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    quiz_session_id         BIGINT          NOT NULL                         COMMENT '소속 퀴즈 세션 ID',
    user_id                 BIGINT          NOT NULL                         COMMENT '소유 사용자 ID (역정규화)',
    subject_id              BIGINT          NOT NULL                         COMMENT '출제 과목 ID (역정규화, NOT NULL 명세 준수)',
    chapter_id              BIGINT          NOT NULL                         COMMENT '출제 챕터 ID (역정규화, NOT NULL 명세 준수)',
    part_id                 BIGINT          NOT NULL                         COMMENT '출제 파트 ID (역정규화, NOT NULL 명세 준수)',

    -- 문항 내용
    question_type           VARCHAR(20)     NOT NULL                         COMMENT '문항 유형: multiple_choice / ox',
    difficulty              VARCHAR(10)     NOT NULL                         COMMENT '문항 난이도: easy / medium / hard',
    body                    TEXT            NOT NULL                         COMMENT '문항 본문',
    summary                 VARCHAR(20)      NULL                            COMMENT 'AI 생성 문항 한줄 요약 8~15자 (인덱스 보드·목록 표시용)',
    correct_explanation     TEXT            NULL                             COMMENT '정답해설',
    incorrect_explanation   TEXT            NULL                             COMMENT '오답해설',
    display_order           TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '세션 내 문항 순서',

    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

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


-- ============================================================
-- QUESTION OPTIONS (선택지) — 객관식 전용
-- ============================================================

-- 선택지: 객관식 문항의 보기 목록
-- OX 문항은 선택지 레코드 없이 question_answers.answer_value = 'O' or 'X'로 처리
CREATE TABLE question_options (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    question_id             BIGINT          NOT NULL                         COMMENT '문항 ID',
    option_number           TINYINT UNSIGNED NOT NULL                        COMMENT '보기 번호 (1~5)',
    content                 TEXT            NOT NULL                         COMMENT '선택지 내용',
    is_correct              TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '정답 여부',

    PRIMARY KEY (id),
    UNIQUE KEY uq_question_options_question_option (question_id, option_number),
    KEY idx_question_options_question_id (question_id),

    CONSTRAINT chk_question_options_is_correct
        CHECK (is_correct IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='객관식 문항 선택지';


-- ============================================================
-- QUESTION ANSWERS (정답 키) — 문항별 정답 정보
-- ============================================================

-- 정답: 문항 유형별 정답 데이터를 단일 테이블로 통합
-- 객관식: answer_value = '2' (정답 보기 번호)
-- OX     : answer_value = 'O' or 'X'
CREATE TABLE question_answers (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    question_id             BIGINT          NOT NULL                         COMMENT '문항 ID',
    answer_value            VARCHAR(10)     NOT NULL                         COMMENT '정답값 (객관식: 보기번호, OX: O/X)',

    PRIMARY KEY (id),
    UNIQUE KEY uq_question_answers_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='문항 정답 키';


-- ============================================================
-- QUIZ GENERATION JOBS (생성 작업 이력) — 비동기 추적
-- ============================================================

-- 생성 작업 이력: 백그라운드 AI 생성의 상세 진행 이력
-- quiz_sessions.status와 별도로 단계별 진행률·재시도 횟수 등을 추적
CREATE TABLE quiz_generation_jobs (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    quiz_session_id     BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'pending',
    progress_pct        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    estimated_seconds   SMALLINT        NULL                    COMMENT '최초 예상 소요 시간(초)',
    estimated_finish_at DATETIME(3)     NULL                    COMMENT '예상 완료 시각 = started_at + estimated_seconds',
    started_at          DATETIME(3)     NULL,
    completed_at        DATETIME(3)     NULL                    COMMENT '실제 완료 시각',
    timeout_at          DATETIME(3)     NULL                    COMMENT '= started_at + 600초',
    retry_count         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    is_retryable        TINYINT(1)      NOT NULL DEFAULT 1,
    fail_code           VARCHAR(50)     NULL,
    fail_reason         VARCHAR(500)    NULL,
    mq_message_id       VARCHAR(100)    NULL    COMMENT 'RabbitMQ message-id (추후 도입 시 채움, MVP는 NULL)',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_generation_jobs_session_id (quiz_session_id),
    KEY idx_quiz_generation_jobs_user_id_status (user_id, status),
    KEY idx_quiz_generation_jobs_timeout_at (timeout_at),

    CONSTRAINT chk_quiz_generation_jobs_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'timeout')),
    CONSTRAINT chk_quiz_generation_jobs_progress_pct
        CHECK (progress_pct BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='퀴즈 AI 생성 작업 이력';

-- ============================================================
-- QUIZ PLAY (퀴즈 풀이) 도메인
-- ============================================================

-- 풀이 세션: 한 번의 퀴즈 풀이 단위
-- 클라이언트가 발급한 풀이 세션 ID를 PK로 사용하지 않고
-- 서버 PK(id) + client_session_id(UK) 구조로 분리
-- → 중복 제출 차단은 client_session_id로, 조인은 id로
CREATE TABLE quiz_play_sessions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK: 서버 풀이 세션 식별자',
    client_session_id       VARCHAR(36)     NOT NULL                         COMMENT '클라이언트 발급 풀이 세션 ID (중복 제출 차단 키)',
    quiz_session_id         BIGINT          NOT NULL                         COMMENT '퀴즈 세션(설정+문항) ID',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID (역정규화)',
    subject_id              BIGINT          NOT NULL                         COMMENT '과목 ID (역정규화)',

    -- 풀이 유형 (HISTORY-001)
    play_type               VARCHAR(20)     NOT NULL DEFAULT 'first'         COMMENT '풀이 유형: first(첫풀이) / retry_all(전체 다시풀기) / retry_wrong(오답 복습)',
    parent_play_session_id  BIGINT          NULL                             COMMENT '부모 풀이 세션 ID (retry_wrong일 때 참조)',
    parent_quiz_session_id  BIGINT          NULL                             COMMENT '부모 퀴즈 세션 ID (자식 세트 생성 시 참조)',
    generation              TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '오답 복습 세대 차수 (0=원본, 1=1차, 2=2차...)',

    -- 풀이 옵션 (HISTORY-005 셔플)
    is_question_shuffled    TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '문제 순서 셔플 여부',
    is_option_shuffled      TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '선택지 순서 셔플 여부',
    shuffle_seed            VARCHAR(100)    NULL                             COMMENT '셔플 시드값 (재진입 시 동일 순서 복원용)',

    -- 풀이 상태
    status                  VARCHAR(20)     NOT NULL DEFAULT 'in_progress'   COMMENT '풀이 상태: in_progress / submitted / abandoned',
    last_question_index     TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '마지막으로 본 문항 인덱스 (앱 재진입 복원용)',

    -- 풀이 시간 (클라이언트 누적값, 백그라운드/모달 시간 제외)
    elapsed_ms              INT UNSIGNED    NOT NULL DEFAULT 0               COMMENT '누적 풀이 시간 (ms, 클라이언트 측정값)',

    -- 제출·완료
    submitted_at            DATETIME(3)     NULL                             COMMENT '결과 제출 시각',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각 (풀이 시작 시각)',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_play_sessions_client_session_id (client_session_id),
    KEY idx_quiz_play_sessions_quiz_session_id (quiz_session_id),
    KEY idx_quiz_play_sessions_user_id_status (user_id, status),
    KEY idx_quiz_play_sessions_user_id_created_at (user_id, created_at),
    KEY idx_quiz_play_sessions_parent_play_session_id (parent_play_session_id),
    KEY idx_quiz_play_sessions_subject_id_created_at (subject_id, created_at),

    CONSTRAINT chk_quiz_play_sessions_play_type
        CHECK (play_type IN ('first', 'retry_all', 'retry_wrong')),
    CONSTRAINT chk_quiz_play_sessions_status
        CHECK (status IN ('in_progress', 'submitted', 'abandoned'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 풀이 세션';


-- 문항별 답안 및 채점 결과
-- 풀이 중 로컬 저장 후 결과 제출 시 일괄 서버 저장
CREATE TABLE quiz_play_answers (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    play_session_id         BIGINT          NOT NULL                         COMMENT '풀이 세션 ID',
    question_id             BIGINT          NOT NULL                         COMMENT '문항 ID',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID (역정규화)',

    -- 답안
    selected_option_id      BIGINT          NULL                             COMMENT '선택한 선택지 ID (객관식, NULL이면 미선택)',
    selected_value          VARCHAR(10)     NULL                             COMMENT '선택한 값 (OX: O/X, 미선택이면 NULL)',

    -- 채점
    is_correct_client       TINYINT(1)      NULL                             COMMENT '클라이언트 채점 결과 (NULL이면 미채점)',
    is_correct_server       TINYINT(1)      NULL                             COMMENT '서버 재채점 결과 (제출 후 확정)',
    is_skipped              TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '미선택 제출 여부 (시간 종료 또는 스킵)',

    -- 풀이 시간
    answer_elapsed_ms       INT UNSIGNED    NULL                             COMMENT '해당 문항 응답 소요 시간 (ms, 타이머 설정 시)',

    -- 한번에 모드 마킹
    is_marked               TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '어려움 마킹 여부 (한번에 모드 임시 마커, 결과화면 후 의미없음)',

    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_play_answers_session_question (play_session_id, question_id),
    KEY idx_quiz_play_answers_question_id (question_id),
    KEY idx_quiz_play_answers_user_id (user_id),

    CONSTRAINT chk_quiz_play_answers_is_correct_client
        CHECK (is_correct_client IN (0, 1) OR is_correct_client IS NULL),
    CONSTRAINT chk_quiz_play_answers_is_correct_server
        CHECK (is_correct_server IN (0, 1) OR is_correct_server IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='문항별 답안 및 채점 결과';


-- ============================================================
-- QUIZ RESULT (퀴즈 결과) 도메인
-- ============================================================

-- 결과 요약: 풀이 세션당 1개, 결과 제출 API와 동일 트랜잭션으로 저장
CREATE TABLE quiz_results (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    public_id               CHAR(36)        NOT NULL                         COMMENT '외부 노출용 UUID v7',
    play_session_id         BIGINT          NOT NULL                         COMMENT '풀이 세션 ID',
    quiz_session_id         BIGINT          NOT NULL                         COMMENT '퀴즈 세션 ID (역정규화)',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID (역정규화)',
    subject_id              BIGINT          NOT NULL                         COMMENT '과목 ID (역정규화 — 기록 목록 필터용)',

    -- 채점 (서버 재채점 기준)
    total_count             TINYINT UNSIGNED NOT NULL                        COMMENT '총 문항 수',
    correct_count           TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '정답 수',
    wrong_count             TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '오답 수',
    skip_count              TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '미선택 수',
    accuracy_pct            TINYINT UNSIGNED NOT NULL DEFAULT 0              COMMENT '정답률 % (소수점 버림)',

    -- 풀이 시간
    elapsed_ms              INT UNSIGNED    NOT NULL DEFAULT 0               COMMENT '누적 풀이 시간 (ms, 클라이언트 전송값)',

    -- 보상 (서버 재채점 기준으로 적립)
    dotori_earned           SMALLINT UNSIGNED NOT NULL DEFAULT 0             COMMENT '이번 풀이로 적립된 도토리',
    xp_earned               SMALLINT UNSIGNED NOT NULL DEFAULT 0             COMMENT '이번 풀이로 적립된 XP',
    is_leveled_up           TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '레벨업 발생 여부',
    new_level               TINYINT UNSIGNED NULL                            COMMENT '레벨업 시 새 레벨 (레벨업 없으면 NULL)',

    -- 검증 플래그
    is_score_matched        TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '클라이언트-서버 채점 일치 여부',
    is_abuse_flagged        TINYINT(1)      NOT NULL DEFAULT 0               COMMENT '어뷰징 의심 플래그 (30% 이상 불일치)',

    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '결과 저장 시각',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_quiz_results_public_id (public_id),
    UNIQUE KEY uq_quiz_results_play_session_id (play_session_id),
    KEY idx_quiz_results_user_id_created_at (user_id, created_at),
    KEY idx_quiz_results_quiz_session_id (quiz_session_id),
    KEY idx_quiz_results_subject_id_created_at (subject_id, created_at),
    KEY idx_quiz_results_is_abuse_flagged (is_abuse_flagged)    -- 어뷰징 모니터링
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='퀴즈 풀이 결과 요약';


-- ============================================================
-- GAMIFICATION (게임화) 도메인
-- ============================================================

-- XP 적립 이력: 모든 XP 적립/변동의 원장
-- users.xp (현재 누적 XP) 와 별개로 이력 보관
CREATE TABLE user_xp_logs (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID',
    play_session_id         BIGINT          NULL                             COMMENT '풀이 세션 ID (풀이 보상일 때, 업로드 보상이면 NULL)',
    lecture_upload_id       BIGINT          NULL                             COMMENT '업로드 ID (업로드 보상일 때)',
    xp_type                 VARCHAR(30)     NOT NULL                         COMMENT 'XP 유형: quiz_correct / quiz_retry / upload / streak_bonus',
    base_xp                 SMALLINT UNSIGNED NOT NULL DEFAULT 0             COMMENT '기본 XP (배수 적용 전)',
    streak_multiplier       DECIMAL(3,1)    NOT NULL DEFAULT 1.0             COMMENT '연속 학습 배수 (1.0 ~ 2.5)',
    earned_xp               SMALLINT UNSIGNED NOT NULL DEFAULT 0             COMMENT '실제 적립 XP (base_xp * streak_multiplier, 소수점 버림)',
    xp_before               INT UNSIGNED    NOT NULL DEFAULT 0               COMMENT '적립 전 누적 XP',
    xp_after                INT UNSIGNED    NOT NULL DEFAULT 0               COMMENT '적립 후 누적 XP',
    level_before            TINYINT UNSIGNED NOT NULL DEFAULT 1              COMMENT '적립 전 레벨',
    level_after             TINYINT UNSIGNED NOT NULL DEFAULT 1              COMMENT '적립 후 레벨',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '적립 시각',

    PRIMARY KEY (id),
    KEY idx_user_xp_logs_user_id_created_at (user_id, created_at),
    KEY idx_user_xp_logs_play_session_id (play_session_id),

    CONSTRAINT chk_user_xp_logs_xp_type
        CHECK (xp_type IN ('quiz_correct', 'quiz_retry', 'upload', 'streak_bonus'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XP 적립 이력';


-- 도토리 적립/차감 이력: 도토리 원장
-- users.dotori_balance (현재 잔액) 와 별개로 이력 보관
CREATE TABLE user_dotori_logs (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID',
    play_session_id         BIGINT          NULL                             COMMENT '풀이 세션 ID (획득일 때)',
    change_type             VARCHAR(20)     NOT NULL                         COMMENT '변동 유형: earn_quiz / spend_item',
    amount                  SMALLINT        NOT NULL                         COMMENT '변동량 (획득: 양수, 차감: 음수)',
    balance_before          INT             NOT NULL                         COMMENT '변동 전 잔액',
    balance_after           INT             NOT NULL                         COMMENT '변동 후 잔액',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '변동 시각',

    PRIMARY KEY (id),
    KEY idx_user_dotori_logs_user_id_created_at (user_id, created_at),
    KEY idx_user_dotori_logs_play_session_id (play_session_id),

    CONSTRAINT chk_user_dotori_logs_change_type
        CHECK (change_type IN ('earn_quiz', 'spend_item')),
    CONSTRAINT chk_user_dotori_logs_balance_after
        CHECK (balance_after >= 0)                                           -- 잔액 음수 방지
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도토리 적립/차감 이력';


-- 연속 학습 이력: 연속 학습 일수 및 배수 산출 원장
-- XP 배수 계산의 근거 데이터
CREATE TABLE user_streak_logs (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID',
    study_date              DATE            NOT NULL                         COMMENT '학습 날짜 (KST 기준)',
    streak_count            SMALLINT UNSIGNED NOT NULL DEFAULT 1             COMMENT '해당 날짜 기준 연속 학습 일수',
    multiplier              DECIMAL(3,1)    NOT NULL DEFAULT 1.0             COMMENT '적용된 XP 배수',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '기록 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_streak_logs_user_study_date (user_id, study_date),   -- 날짜당 1개
    KEY idx_user_streak_logs_user_id_study_date (user_id, study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연속 학습 이력';


-- ============================================================
-- MY PAGE (마이페이지) 도메인
-- ============================================================

-- 알림 설정: 사용자별 카테고리별 ON/OFF
CREATE TABLE user_notification_settings (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    user_id                 BIGINT          NOT NULL                         COMMENT '사용자 ID',
    fcm_token               VARCHAR(255)    NULL                             COMMENT 'FCM 푸시 토큰 (최신 기기 토큰)',
    is_activity_enabled     TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '활동 알림 (퀴즈 생성, 업로드 완료 등)',
    is_update_enabled       TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '업데이트/공지 알림',
    is_review_enabled       TINYINT(1)      NOT NULL DEFAULT 1               COMMENT '복습 주기 알림',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '생성 시각',
    updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_notification_settings_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 알림 설정';


-- 피드백/문의
CREATE TABLE user_feedbacks (
    id                      BIGINT          NOT NULL AUTO_INCREMENT          COMMENT 'PK',
    user_id                 BIGINT          NULL                             COMMENT '사용자 ID (비로그인이면 NULL)',
    category                VARCHAR(20)     NOT NULL                         COMMENT '카테고리: feature / bug / inquiry / other',
    body                    VARCHAR(1000)   NOT NULL                         COMMENT '본문 (1~1000자)',
    reply_email             VARCHAR(255)    NULL                             COMMENT '회신 이메일',
    app_version             VARCHAR(20)     NULL                             COMMENT '앱 버전',
    os_version              VARCHAR(50)     NULL                             COMMENT 'OS 버전',
    device_model            VARCHAR(100)    NULL                             COMMENT '디바이스 모델',
    created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '제출 시각',

    PRIMARY KEY (id),
    KEY idx_user_feedbacks_user_id (user_id),
    KEY idx_user_feedbacks_category_created_at (category, created_at),

    CONSTRAINT chk_user_feedbacks_category
        CHECK (category IN ('feature', 'bug', 'inquiry', 'other')),
    CONSTRAINT chk_user_feedbacks_body_length
        CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 피드백/문의';
