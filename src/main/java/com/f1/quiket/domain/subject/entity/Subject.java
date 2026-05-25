package com.f1.quiket.domain.subject.entity;

import com.f1.quiket.global.entity.BaseEntity;
import com.f1.quiket.global.util.UuidV7Generator;
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
 * 과목 엔티티
 */
@Entity
@Table(
        name = "subjects",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subjects_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_subjects_user_id_deleted_at", columnList = "user_id, deleted_at"),
                @Index(name = "idx_subjects_user_id_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subject extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "name", length = 30, nullable = false)
    String name;

    @Column(name = "purpose", length = 20, nullable = false)
    String purpose;

    /**
     * 과목 생성
     */
    public static Subject create(Long userId, String name, String purpose) {
        Subject subject = new Subject();
        subject.publicId = UuidV7Generator.generate();
        subject.userId = userId;
        subject.name = name;
        subject.purpose = purpose;
        return subject;
    }

    /**
     * 과목명 변경
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * 과목 상세 유형 변경
     */
    public void updatePurpose(String purpose) {
        this.purpose = purpose;
    }
}
