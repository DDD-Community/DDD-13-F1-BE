-- soft delete 대상 테이블 삭제 시각 컬럼 추가
SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'certificates'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'certificates'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE certificates ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'lecture_uploads'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'lecture_uploads'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE lecture_uploads ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'lecture_upload_files'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'lecture_upload_files'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE lecture_upload_files ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'questions'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'questions'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE questions ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_play_sessions'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_play_sessions'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE quiz_play_sessions ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_results'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'quiz_results'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE quiz_results ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER updated_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deleted_at_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'user_feedbacks'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_feedbacks'
              AND column_name = 'deleted_at'
        ),
        'ALTER TABLE user_feedbacks ADD COLUMN deleted_at DATETIME(3) NULL COMMENT ''삭제 시각'' AFTER created_at',
        'SELECT 1'
    )
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
