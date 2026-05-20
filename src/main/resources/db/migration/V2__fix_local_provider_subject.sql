-- local 인증 수단 provider_subject 보정
UPDATE user_auth_identities uai
INNER JOIN users u ON uai.user_id = u.id
SET uai.provider_subject = u.public_id
WHERE uai.provider = 'local'
  AND uai.provider_subject = '';

ALTER TABLE user_auth_identities
    MODIFY provider_subject VARCHAR(255) NOT NULL DEFAULT ''
        COMMENT '로그인 수단별 사용자 고유 식별값, local은 users.public_id';
