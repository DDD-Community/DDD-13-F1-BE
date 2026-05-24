package com.f1.quiket.domain.part.entity;

import com.f1.quiket.global.entity.BaseEntity;
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
 * 파트 엔티티
 */
@Entity
@Table(
        name = "parts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_parts_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_parts_subject_id_deleted_at", columnList = "subject_id, deleted_at"),
                @Index(name = "idx_parts_user_id_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Part extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "chapter_id", nullable = false)
    Long chapterId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "name", length = 100, nullable = false)
    String name;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Lob
    @Column(name = "content")
    String content;
}
