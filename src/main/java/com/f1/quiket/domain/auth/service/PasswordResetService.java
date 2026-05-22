package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.PasswordResetConfirmRequest;
import com.f1.quiket.domain.auth.dto.PasswordResetRequest;
import com.f1.quiket.domain.auth.dto.PasswordResetRequestedResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.entity.UserPasswordResetToken;
import com.f1.quiket.domain.auth.event.PasswordResetMailRequestedEvent;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.repository.UserPasswordResetTokenRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private static final String PROVIDER_LOCAL = "local";
    private static final String RESET_STATUS_PENDING = "pending";
    private static final String RESET_STATUS_EXPIRED = "expired";
    private static final String RESET_STATUS_CANCELLED = "cancelled";
    private static final long PASSWORD_RESET_TTL_SECONDS = 600L;

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final UserPasswordResetTokenRepository userPasswordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeGenerator verificationCodeGenerator;
    private final AuthTokenService authTokenService;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetRequestedResponse requestPasswordReset(
            PasswordResetRequest request,
            AuthTokenRequestContext context
    ) {
        User user = findUserByEmail(request.getEmail());
        findLocalIdentity(user);
        return createPasswordResetToken(user, context.getIpAddress());
    }

    public PasswordResetRequestedResponse requestPasswordResetForLockedUser(User user, String requestedIp) {
        findLocalIdentity(user);
        return createPasswordResetToken(user, requestedIp);
    }

    @Transactional(noRollbackFor = CustomException.class)
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        validatePasswordConfirm(request.getNewPassword(), request.getNewPasswordConfirm());

        User user = findUserByEmail(request.getEmail());
        UserAuthIdentity localIdentity = findLocalIdentity(user);
        UserPasswordResetToken resetToken = findPendingResetToken(user, request);
        LocalDateTime now = LocalDateTime.now();

        if (resetToken.isExpired(now)) {
            resetToken.expire();
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_EXPIRED);
        }

        if (passwordEncoder.matches(request.getNewPassword(), localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_SAME_AS_PREVIOUS);
        }

        localIdentity.changePassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.use(now);
        user.unlock();
        authTokenService.revokeAllActiveRefreshTokens(user);
    }

    public int expirePendingPasswordResetTokens() {
        return userPasswordResetTokenRepository.expirePendingResetTokens(
                RESET_STATUS_PENDING,
                RESET_STATUS_EXPIRED,
                LocalDateTime.now()
        );
    }

    private PasswordResetRequestedResponse createPasswordResetToken(User user, String requestedIp) {
        userPasswordResetTokenRepository.cancelPendingResetTokens(
                user,
                RESET_STATUS_PENDING,
                RESET_STATUS_CANCELLED
        );

        String verificationCode = verificationCodeGenerator.generate();
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(PASSWORD_RESET_TTL_SECONDS);

        UserPasswordResetToken passwordResetToken = UserPasswordResetToken.create(
                user,
                resetToken,
                verificationCode,
                requestedIp,
                expiresAt
        );
        userPasswordResetTokenRepository.save(passwordResetToken);

        eventPublisher.publishEvent(new PasswordResetMailRequestedEvent(user.getEmail(), verificationCode));
        return PasswordResetRequestedResponse.of(user.getEmail(), PASSWORD_RESET_TTL_SECONDS);
    }

    private UserPasswordResetToken findPendingResetToken(User user, PasswordResetConfirmRequest request) {
        if (StringUtils.hasText(request.getVerificationCode())) {
            return userPasswordResetTokenRepository
                    .findTopByUserAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            user,
                            request.getVerificationCode(),
                            RESET_STATUS_PENDING
                    )
                    .orElseThrow(() -> new CustomException(ErrorCode.AUTH_PASSWORD_RESET_INVALID));
        }

        if (StringUtils.hasText(request.getResetToken())) {
            return userPasswordResetTokenRepository
                    .findTopByUserAndResetTokenAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            user,
                            request.getResetToken(),
                            RESET_STATUS_PENDING
                    )
                    .orElseThrow(() -> new CustomException(ErrorCode.AUTH_PASSWORD_RESET_INVALID));
        }

        throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_INVALID);
    }

    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private UserAuthIdentity findLocalIdentity(User user) {
        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));

        if (!StringUtils.hasText(localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND);
        }
        return localIdentity;
    }
}
