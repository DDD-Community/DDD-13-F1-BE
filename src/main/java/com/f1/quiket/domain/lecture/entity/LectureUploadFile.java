package com.f1.quiket.domain.lecture.entity;

import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 강의 업로드 파일 메타데이터 엔티티
 *
 * PDF 또는 이미지 파일의 원본명, 크기, 순서, OCR 상태를 보관
 */
@Entity
@Table(
        name = "lecture_upload_files",
        indexes = {
                @Index(
                        name = "idx_lecture_upload_files_lecture_upload_id_order",
                        columnList = "lecture_upload_id, display_order"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LectureUploadFile extends BaseEntity {

    @Column(name = "lecture_upload_id", nullable = false)
    Long lectureUploadId;

    @Column(name = "file_url", length = 500, nullable = false)
    String fileUrl;

    @Column(name = "file_name", length = 255, nullable = false)
    String fileName;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "file_type", length = 20, nullable = false)
    String fileType;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    @Column(name = "ocr_status", length = 20)
    String ocrStatus;

    /**
     * 업로드 파일 메타데이터 생성
     */
    public static LectureUploadFile create(
            Long lectureUploadId,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileType,
            Integer displayOrder,
            String ocrStatus
    ) {
        LectureUploadFile file = new LectureUploadFile();
        file.lectureUploadId = lectureUploadId;
        file.fileUrl = fileUrl;
        file.fileName = fileName;
        file.fileSize = fileSize;
        file.fileType = fileType;
        file.displayOrder = displayOrder;
        file.ocrStatus = ocrStatus;
        return file;
    }

    /**
     * OCR 성공 상태 변경
     */
    public void markOcrSuccess() {
        this.ocrStatus = "success";
    }

    /**
     * OCR 실패 상태 변경
     */
    public void markOcrFailed() {
        this.ocrStatus = "failed";
    }
}
