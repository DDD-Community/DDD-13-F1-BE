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
 * 사용자 인증 수단 엔티티
 */
@Entity
@Table(
        name = "user_auth_identities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_auth_identities_provider_subject",
                        columnNames = {"provider", "provider_subject"}
                ),
                @UniqueConstraint(
                        name = "uq_user_auth_identities_user_id_provider",
                        columnNames = {"user_id", "provider"}
                )
        },
        indexes = {
                @Index(name = "idx_user_auth_identities_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_auth_identities_provider_created_at", columnList = "provider, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAuthIdentity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_auth_identities_user_id")
    )
    User user;

    @Column(name = "provider", length = 20, nullable = false)
    String provider;

    @Column(name = "provider_subject", length = 255, nullable = false)
    String providerSubject = "";

    @Column(name = "password_hash", length = 255)
    String passwordHash;

    @Column(name = "is_primary", nullable = false)
    boolean primary = false;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    public static UserAuthIdentity createLocal(User user, String passwordHash, boolean primary) {
        UserAuthIdentity identity = new UserAuthIdentity();
        identity.user = user;
        identity.provider = "local";
        identity.providerSubject = user.getPublicId();
        identity.passwordHash = passwordHash;
        identity.primary = primary;
        return identity;
    }

    public static UserAuthIdentity createOAuth(User user, String provider, String providerSubject, boolean primary) {
        UserAuthIdentity identity = new UserAuthIdentity();
        identity.user = user;
        identity.provider = provider;
        identity.providerSubject = providerSubject;
        identity.primary = primary;
        return identity;
    }

    public void recordLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
