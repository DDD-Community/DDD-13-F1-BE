-- users.email 선택값 전환
-- 소셜(kakao) 전용 계정의 이메일 미보유 허용, uq_users_email은 다중 NULL 허용으로 유지
ALTER TABLE users
    MODIFY email VARCHAR(255) NULL COMMENT '사용자 이메일, 소셜 전용 계정은 NULL';
