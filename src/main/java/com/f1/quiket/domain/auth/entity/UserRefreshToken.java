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
 * 사용자 리프레시 토큰 엔티티
 */
@Entity
@Table(
        name = "user_refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_refresh_tokens_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(name = "idx_user_refresh_tokens_user_id_expires_at", columnList = "user_id, expires_at"),
                @Index(name = "idx_user_refresh_tokens_user_id_revoked_at", columnList = "user_id, revoked_at"),
                @Index(name = "idx_user_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_refresh_tokens_user_id")
    )
    User user;

    @Column(name = "token_hash", length = 255, nullable = false)
    String tokenHash;

    @Column(name = "device_id", length = 128)
    String deviceId;

    @Column(name = "device_name", length = 100)
    String deviceName;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "issued_at", nullable = false)
    LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @Column(name = "last_used_at", nullable = false)
    LocalDateTime lastUsedAt = LocalDateTime.now();

    public static UserRefreshToken create(
            User user,
            String tokenHash,
            String deviceId,
            String deviceName,
            String userAgent,
            String ipAddress,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.user = user;
        refreshToken.tokenHash = tokenHash;
        refreshToken.deviceId = deviceId;
        refreshToken.deviceName = deviceName;
        refreshToken.userAgent = userAgent;
        refreshToken.ipAddress = ipAddress;
        refreshToken.issuedAt = issuedAt;
        refreshToken.expiresAt = expiresAt;
        refreshToken.lastUsedAt = issuedAt;
        return refreshToken;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void recordUsed(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
