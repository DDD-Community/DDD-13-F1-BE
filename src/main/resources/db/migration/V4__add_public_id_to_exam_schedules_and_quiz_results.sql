-- 시험 일정, 퀴즈 결과 외부 노출용 public_id 추가
-- 기존 데이터 대응을 위해 NULL 허용 후, UUID v7로 채우고 NOT NULL로 변경

ALTER TABLE subject_exam_schedules
    ADD COLUMN public_id CHAR(36) NULL COMMENT '외부 노출용 UUID v7' AFTER id;

UPDATE subject_exam_schedules
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE subject_exam_schedules
    MODIFY public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    ADD UNIQUE KEY uq_subject_exam_schedules_public_id (public_id);

ALTER TABLE quiz_results
    ADD COLUMN public_id CHAR(36) NULL COMMENT '외부 노출용 UUID v7' AFTER id;

UPDATE quiz_results
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE quiz_results
    MODIFY public_id CHAR(36) NOT NULL COMMENT '외부 노출용 UUID v7',
    ADD UNIQUE KEY uq_quiz_results_public_id (public_id);
