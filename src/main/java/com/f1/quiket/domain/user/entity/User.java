package com.f1.quiket.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    String publicId;

    @Column(length = 255, nullable = false, unique = true)
    String email;

    @Column(length = 36, nullable = false)
    String nickname;

    @Column(length = 20, nullable = false)
    String status;

    @Column(name = "is_email_verified", nullable = false)
    boolean emailVerified;

    @Column(name = "failed_login_count", nullable = false)
    Integer failedLoginCount;

    @Column(name = "last_failed_login_at")
    LocalDateTime lastFailedLoginAt;

    @Column(name = "locked_at")
    LocalDateTime lockedAt;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    String lastLoginIp;

    @Column(name = "dotori_balance", nullable = false)
    Integer dotoriBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}
