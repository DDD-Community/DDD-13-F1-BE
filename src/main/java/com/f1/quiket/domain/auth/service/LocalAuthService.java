package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.EmailAvailabilityResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.dto.LoginFailureResponse;
import com.f1.quiket.domain.auth.dto.LoginRequest;
import com.f1.quiket.domain.auth.dto.SignupRequest;
import com.f1.quiket.domain.auth.dto.SignupResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.entity.UserEmailVerification;
import com.f1.quiket.domain.auth.event.EmailVerificationMailRequestedEvent;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.repository.UserEmailVerificationRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
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
public class LocalAuthService {

    private static final String PROVIDER_LOCAL = "local";
    private static final String VERIFICATION_STATUS_PENDING = "pending";
    private static final String VERIFICATION_STATUS_EXPIRED = "expired";
    private static final String VERIFICATION_STATUS_CANCELLED = "cancelled";
    private static final long EMAIL_VERIFICATION_TTL_SECONDS = 600L;
    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final UserEmailVerificationRepository userEmailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeGenerator verificationCodeGenerator;
    private final AuthTokenService authTokenService;
    private final PasswordResetService passwordResetService;
    private final ApplicationEventPublisher eventPublisher;

    public SignupResponse signup(SignupRequest request) {
        validatePasswordConfirm(request.getPassword(), request.getPasswordConfirm());
        validateEmailNotExists(request.getEmail());

        User user = User.create(UuidV7Generator.generate(), request.getEmail(), request.getNickname());
        User savedUser = userRepository.save(user);

        UserAuthIdentity identity = UserAuthIdentity.createLocal(
                savedUser,
                passwordEncoder.encode(request.getPassword()),
                true
        );
        userAuthIdentityRepository.save(identity);

        sendEmailVerification(savedUser);
        return SignupResponse.of(savedUser, true);
    }

    @Transactional(readOnly = true)
    public EmailAvailabilityResponse checkEmailAvailability(String email) {
        boolean available = !userRepository.existsByEmail(email);
        return EmailAvailabilityResponse.of(email, available);
    }

    public EmailVerificationSentResponse resendEmailVerification(String email) {
        User user = findUserByEmail(email);
        sendEmailVerification(user);
        return EmailVerificationSentResponse.of(email, EMAIL_VERIFICATION_TTL_SECONDS);
    }

    @Transactional(noRollbackFor = CustomException.class)
    public AuthTokenResponse confirmEmailVerification(
            EmailVerificationConfirmRequest request,
            AuthTokenRequestContext context
    ) {
        UserEmailVerification verification = findPendingVerification(request);
        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            verification.expire();
            throw new CustomException(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED);
        }

        User user = verification.getUser();
        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));

        verification.verify(now);
        user.verifyEmail();
        user.recordLoginSuccess(context.getIpAddress());
        localIdentity.recordLoginSuccess();
        return authTokenService.issueTokens(user, context);
    }

    @Transactional(noRollbackFor = CustomException.class)
    public AuthTokenResponse login(LoginRequest request, AuthTokenRequestContext context) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        validateAccountIsNotLocked(user);

        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));

        validatePassword(user, localIdentity, request.getPassword(), context);
        validateEmailVerified(user);

        user.recordLoginSuccess(context.getIpAddress());
        localIdentity.recordLoginSuccess();
        return authTokenService.issueTokens(user, context);
    }

    public int expirePendingEmailVerifications() {
        return userEmailVerificationRepository.expirePendingVerifications(
                VERIFICATION_STATUS_PENDING,
                VERIFICATION_STATUS_EXPIRED,
                LocalDateTime.now()
        );
    }

    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private void validateAccountIsNotLocked(User user) {
        if (user.isLocked()) {
            throw loginFailureException(ErrorCode.AUTH_ACCOUNT_LOCKED, user, false);
        }
    }

    private void validatePassword(
            User user,
            UserAuthIdentity localIdentity,
            String password,
            AuthTokenRequestContext context
    ) {
        if (!StringUtils.hasText(localIdentity.getPasswordHash())
                || !passwordEncoder.matches(password, localIdentity.getPasswordHash())) {
            user.recordLoginFailure(MAX_FAILED_LOGIN_COUNT);
            if (user.isLocked()) {
                passwordResetService.requestPasswordResetForLockedUser(user, context.getIpAddress());
                throw loginFailureException(ErrorCode.AUTH_ACCOUNT_LOCKED, user, true);
            }
            throw loginFailureException(ErrorCode.AUTH_INVALID_CREDENTIALS, user, false);
        }
    }

    private void validateEmailVerified(User user) {
        if (!user.isEmailVerified()) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }

    private void sendEmailVerification(User user) {
        userEmailVerificationRepository.cancelPendingVerifications(
                user,
                VERIFICATION_STATUS_PENDING,
                VERIFICATION_STATUS_CANCELLED
        );

        String verificationCode = verificationCodeGenerator.generate();
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(EMAIL_VERIFICATION_TTL_SECONDS);

        UserEmailVerification verification = UserEmailVerification.create(
                user,
                user.getEmail(),
                verificationToken,
                verificationCode,
                expiresAt
        );
        userEmailVerificationRepository.save(verification);

        eventPublisher.publishEvent(new EmailVerificationMailRequestedEvent(user.getEmail(), verificationCode));
    }

    private UserEmailVerification findPendingVerification(EmailVerificationConfirmRequest request) {
        if (StringUtils.hasText(request.getVerificationCode())) {
            return userEmailVerificationRepository
                    .findTopByEmailAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            request.getEmail(),
                            request.getVerificationCode(),
                            VERIFICATION_STATUS_PENDING
                    )
                    .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_EMAIL_VERIFICATION));
        }

        if (StringUtils.hasText(request.getVerificationToken())) {
            return userEmailVerificationRepository
                    .findTopByEmailAndVerificationTokenAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            request.getEmail(),
                            request.getVerificationToken(),
                            VERIFICATION_STATUS_PENDING
                    )
                    .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_EMAIL_VERIFICATION));
        }

        throw new CustomException(ErrorCode.AUTH_INVALID_EMAIL_VERIFICATION);
    }

    private CustomException loginFailureException(ErrorCode errorCode, User user, boolean resetCodeSent) {
        LoginFailureResponse response = LoginFailureResponse.of(user, MAX_FAILED_LOGIN_COUNT, resetCodeSent);
        return new CustomException(errorCode, resolveLoginFailureMessage(errorCode), response);
    }

    private String resolveLoginFailureMessage(ErrorCode errorCode) {
        // 잠금 케이스만 OpenAPI 계약에 맞춰 재설정 안내 문구를 덧붙여 응답
        if (errorCode == ErrorCode.AUTH_ACCOUNT_LOCKED) {
            return errorCode.getMessage() + " 비밀번호 재설정을 진행해주세요.";
        }
        return errorCode.getMessage();
    }
}
