ALTER TABLE user_password_reset_tokens
    ADD COLUMN verification_code VARCHAR(20) NULL COMMENT '사용자 입력용 인증 코드' AFTER reset_token;

CREATE INDEX idx_user_password_reset_tokens_user_id_code_status
    ON user_password_reset_tokens (user_id, verification_code, status);
