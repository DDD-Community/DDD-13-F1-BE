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
    xp_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '누적 XP 총량',
    current_level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '현재 레벨',
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
    provider_subject VARCHAR(255) NOT NULL DEFAULT '' COMMENT '소셜 로그인 고유 식별값, local은 빈 문자열',
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

    CONSTRAINT fk_user_auth_identities_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
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

    CONSTRAINT fk_user_email_verifications_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
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

    CONSTRAINT fk_user_password_reset_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
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
    KEY idx_user_refresh_tokens_expires_at (expires_at),

    CONSTRAINT fk_user_refresh_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 리프레시 토큰 관리';
