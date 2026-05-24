package com.f1.quiket.domain.auth.entity;

import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * 비밀번호 재설정 토큰 엔티티
 */
@Entity
@Table(
        name = "user_password_reset_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_password_reset_tokens_reset_token",
                        columnNames = "reset_token"
                )
        },
        indexes = {
                @Index(name = "idx_user_password_reset_tokens_user_id_status", columnList = "user_id, status"),
                @Index(name = "idx_user_password_reset_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_password_reset_tokens_user_id")
    )
    User user;

    @Column(name = "reset_token", length = 255, nullable = false)
    String resetToken;

    @Column(name = "verification_code", length = 20)
    String verificationCode;

    @Column(name = "status", length = 20, nullable = false)
    String status = "pending";

    @Column(name = "requested_ip", length = 45)
    String requestedIp;

    @Column(name = "requested_at", nullable = false)
    LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "used_at")
    LocalDateTime usedAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    public static UserPasswordResetToken create(
            User user,
            String resetToken,
            String verificationCode,
            String requestedIp,
            LocalDateTime expiresAt
    ) {
        UserPasswordResetToken passwordResetToken = new UserPasswordResetToken();
        passwordResetToken.user = user;
        passwordResetToken.resetToken = resetToken;
        passwordResetToken.verificationCode = verificationCode;
        passwordResetToken.requestedIp = requestedIp;
        passwordResetToken.expiresAt = expiresAt;
        return passwordResetToken;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void use(LocalDateTime now) {
        this.status = "used";
        this.usedAt = now;
    }

    public void expire() {
        this.status = "expired";
    }

    public void cancel() {
        this.status = "cancelled";
    }
}
