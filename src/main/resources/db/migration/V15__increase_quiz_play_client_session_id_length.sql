ALTER TABLE quiz_play_sessions
    MODIFY COLUMN client_session_id VARCHAR(128) NOT NULL COMMENT '클라이언트 발급 풀이 세션 ID';
