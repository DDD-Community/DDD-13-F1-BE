package com.f1.quiket.domain.auth.repository;

import com.f1.quiket.domain.auth.entity.UserPasswordResetToken;
import com.f1.quiket.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPasswordResetTokenRepository extends JpaRepository<UserPasswordResetToken, Long> {

    Optional<UserPasswordResetToken> findTopByUserAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            User user,
            String verificationCode,
            String status
    );

    Optional<UserPasswordResetToken> findTopByUserAndResetTokenAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            User user,
            String resetToken,
            String status
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update UserPasswordResetToken resetToken
            set resetToken.status = :cancelledStatus
            where resetToken.user = :user
              and resetToken.status = :pendingStatus
              and resetToken.deletedAt is null
            """)
    int cancelPendingResetTokens(
            @Param("user") User user,
            @Param("pendingStatus") String pendingStatus,
            @Param("cancelledStatus") String cancelledStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserPasswordResetToken resetToken
            set resetToken.status = :expiredStatus
            where resetToken.status = :pendingStatus
              and resetToken.expiresAt <= :now
              and resetToken.deletedAt is null
            """)
    int expirePendingResetTokens(
            @Param("pendingStatus") String pendingStatus,
            @Param("expiredStatus") String expiredStatus,
            @Param("now") LocalDateTime now
    );
}
