-- subject 상세 테이블 soft delete 컬럼 정합성 보강
ALTER TABLE subject_exam_details
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '삭제 시각' AFTER updated_at;

ALTER TABLE subject_review_details
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '삭제 시각' AFTER updated_at;

ALTER TABLE subject_other_details
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '삭제 시각' AFTER updated_at;

