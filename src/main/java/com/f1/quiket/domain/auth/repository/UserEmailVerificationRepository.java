package com.f1.quiket.domain.auth.repository;

import com.f1.quiket.domain.auth.entity.UserEmailVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
