ALTER TABLE user_auth_identities
    ADD COLUMN oauth_refresh_token VARCHAR(512) NULL COMMENT 'OAuth 제공자 refresh token (Apple 탈퇴 revoke용)' AFTER is_primary;
