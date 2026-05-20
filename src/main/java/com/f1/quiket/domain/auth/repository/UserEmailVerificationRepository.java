package com.f1.quiket.domain.auth.repository;

import com.f1.quiket.domain.auth.entity.UserEmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserEmailVerificationRepository extends JpaRepository<UserEmailVerification, Long> {

    Optional<UserEmailVerification> findTopByEmailAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            String email,
            String verificationCode,
            String status
    );

    Optional<UserEmailVerification> findTopByEmailAndVerificationTokenAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            String email,
            String verificationToken,
            String status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEmailVerification verification
            set verification.status = :expiredStatus
            where verification.status = :pendingStatus
              and verification.expiresAt <= :now
              and verification.deletedAt is null
            """)
    int expirePendingVerifications(
            @Param("pendingStatus") String pendingStatus,
            @Param("expiredStatus") String expiredStatus,
            @Param("now") LocalDateTime now
    );
}
