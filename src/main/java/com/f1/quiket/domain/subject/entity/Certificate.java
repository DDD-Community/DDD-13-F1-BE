package com.f1.quiket.domain.subject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 자격증 마스터 엔티티
 */
@Entity
@Table(
        name = "certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_certificates_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_certificates_is_featured_display_order", columnList = "is_featured, display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "name", length = 100, nullable = false)
    String name;

    @Column(name = "is_featured", nullable = false)
    Boolean featured;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
