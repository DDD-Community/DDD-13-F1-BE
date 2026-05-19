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
 * 이메일 인증 요청 이력 엔티티
 */
@Entity
@Table(
        name = "user_email_verifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_email_verifications_verification_token",
                        columnNames = "verification_token"
                )
        },
        indexes = {
                @Index(name = "idx_user_email_verifications_user_id_status", columnList = "user_id, status"),
                @Index(name = "idx_user_email_verifications_email_status", columnList = "email, status"),
                @Index(name = "idx_user_email_verifications_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEmailVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_email_verifications_user_id")
    )
    User user;

    @Column(name = "email", length = 255, nullable = false)
    String email;

    @Column(name = "verification_token", length = 255, nullable = false)
    String verificationToken;

    @Column(name = "verification_code", length = 20)
    String verificationCode;

    @Column(name = "status", length = 20, nullable = false)
    String status = "pending";

    @Column(name = "requested_at", nullable = false)
    LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    public static UserEmailVerification create(
            User user,
            String email,
            String verificationToken,
            String verificationCode,
            LocalDateTime expiresAt
    ) {
        UserEmailVerification verification = new UserEmailVerification();
        verification.user = user;
        verification.email = email;
        verification.verificationToken = verificationToken;
        verification.verificationCode = verificationCode;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void verify(LocalDateTime now) {
        this.status = "verified";
        this.verifiedAt = now;
    }

    public void expire() {
        this.status = "expired";
    }
}
