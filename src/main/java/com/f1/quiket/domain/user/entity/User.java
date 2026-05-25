package com.f1.quiket.domain.user.entity;

import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * 사용자 도메인 엔티티
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_users_deleted_at_created_at", columnList = "deleted_at, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "email", length = 255, nullable = false)
    String email;

    @Column(length = 36, nullable = false)
    String nickname;

    @Column(length = 20, nullable = false)
    String status = "active";

    @Column(name = "is_email_verified", nullable = false)
    boolean emailVerified = false;

    @Column(name = "failed_login_count", nullable = false)
    Integer failedLoginCount = 0;

    @Column(name = "last_failed_login_at")
    LocalDateTime lastFailedLoginAt;

    @Column(name = "locked_at")
    LocalDateTime lockedAt;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    String lastLoginIp;

    @Column(name = "dotori_balance", nullable = false)
    Integer dotoriBalance = 0;

    @Column(name = "xp_total", nullable = false)
    Integer xpTotal = 0;

    @Column(name = "current_level", nullable = false)
    Integer currentLevel = 1;

    public static User create(String publicId, String email, String nickname) {
        User user = new User();
        user.publicId = publicId;
        user.email = email;
        user.nickname = nickname;
        return user;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void applyQuizReward(int dotoriEarned, int xpEarned, int levelAfter) {
        this.dotoriBalance += dotoriEarned;
        this.xpTotal += xpEarned;
        this.currentLevel = levelAfter;
    }

    public void recordLoginFailure(int maxFailedCount) {
        LocalDateTime now = LocalDateTime.now();
        this.failedLoginCount++;
        this.lastFailedLoginAt = now;
        if (this.failedLoginCount >= maxFailedCount) {
            this.status = "locked";
            this.lockedAt = now;
        }
    }

    public void recordLoginSuccess(String loginIp) {
        this.failedLoginCount = 0;
        this.lastFailedLoginAt = null;
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = loginIp;
    }

    public boolean isLocked() {
        return "locked".equals(this.status);
    }

    public void unlock() {
        this.status = "active";
        this.failedLoginCount = 0;
        this.lastFailedLoginAt = null;
        this.lockedAt = null;
    }
}
