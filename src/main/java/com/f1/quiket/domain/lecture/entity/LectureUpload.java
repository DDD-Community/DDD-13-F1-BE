package com.f1.quiket.domain.lecture.entity;

import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.global.entity.BaseEntity;
import com.f1.quiket.global.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 강의 업로드 처리 정보 엔티티
 *
 * 업로드 접수 상태, 추출 원문 보관
 */
@Entity
@Table(
        name = "lecture_uploads",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lecture_uploads_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_lecture_uploads_chapter_id_status", columnList = "chapter_id, status"),
                @Index(name = "idx_lecture_uploads_user_id_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LectureUpload extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "chapter_id", nullable = false)
    Long chapterId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "upload_type", length = 10, nullable = false)
    String uploadType;

    @Column(name = "part_split_method", length = 10, nullable = false)
    String partSplitMethod;

    @Column(name = "status", length = 20, nullable = false)
    String status;

    @Lob
    @Column(name = "raw_text")
    String rawText;

    /**
     * 강의 업로드 접수 정보 생성
     */
    public static LectureUpload create(
            Long chapterId,
            Long userId,
            StudyMaterialUploadType uploadType,
            PartSplitMethod partSplitMethod
    ) {
        LectureUpload upload = new LectureUpload();
        upload.publicId = UuidV7Generator.generate();
        upload.chapterId = chapterId;
        upload.userId = userId;
        upload.uploadType = uploadType.getValue();
        upload.partSplitMethod = partSplitMethod.getValue();
        upload.status = LectureUploadStatus.PENDING.getValue();
        return upload;
    }

    /**
     * 처리 중 상태 변경
     */
    public void markProcessing() {
        this.status = LectureUploadStatus.PROCESSING.getValue();
    }

    /**
     * 처리 완료 상태 변경
     */
    public void markCompleted(String rawText) {
        this.status = LectureUploadStatus.COMPLETED.getValue();
        this.rawText = rawText;
    }

    /**
     * 처리 실패 상태 변경
     */
    public void markFailed() {
        this.status = LectureUploadStatus.FAILED.getValue();
    }
}
