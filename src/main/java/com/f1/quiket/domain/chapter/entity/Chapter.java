package com.f1.quiket.domain.chapter.entity;

import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 챕터 엔티티
 */
@Entity
@Table(
        name = "chapters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_chapters_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_chapters_subject_id_deleted_at", columnList = "subject_id, deleted_at"),
                @Index(name = "idx_chapters_user_id_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Chapter extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "name", length = 30, nullable = false)
    String name;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    /**
     * 챕터명 변경
     */
    public void updateName(String name) {
        this.name = name;
    }
}
