package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.EmailAvailabilityResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.LoginRequest;
import com.f1.quiket.domain.auth.dto.SignupRequest;
import com.f1.quiket.domain.auth.dto.SignupResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.entity.UserEmailVerification;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.repository.UserEmailVerificationRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import com.f1.quiket.infra.mail.service.SesMailSender;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    private static final long EMAIL_VERIFICATION_TTL_SECONDS = 1800L;
    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final UserEmailVerificationRepository userEmailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeGenerator verificationCodeGenerator;
    private final MailTemplateFactory mailTemplateFactory;
    private final SesMailSender sesMailSender;
    private final AuthTokenService authTokenService;

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

    public EmailVerificationConfirmResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        UserEmailVerification verification = findPendingVerification(request);
        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            verification.expire();
            throw new CustomException(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED);
        }

        verification.verify(now);
        verification.getUser().verifyEmail();
        return EmailVerificationConfirmResponse.verified(verification.getEmail());
    }

    public AuthTokenResponse login(LoginRequest request, AuthTokenRequestContext context) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        validateAccountIsNotLocked(user);

        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));

        validatePassword(user, localIdentity, request.getPassword());
        validateEmailVerified(user);

        user.recordLoginSuccess(context.getIpAddress());
        localIdentity.recordLoginSuccess();
        return authTokenService.issueTokens(user, context);
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
            throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    private void validatePassword(User user, UserAuthIdentity localIdentity, String password) {
        if (!StringUtils.hasText(localIdentity.getPasswordHash())
                || !passwordEncoder.matches(password, localIdentity.getPasswordHash())) {
            user.recordLoginFailure(MAX_FAILED_LOGIN_COUNT);
            if (user.isLocked()) {
                throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
            }
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private void validateEmailVerified(User user) {
        if (!user.isEmailVerified()) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }

    private void sendEmailVerification(User user) {
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

        MailSendRequest mailRequest = mailTemplateFactory.createSignUpVerificationMail(user.getEmail(), verificationCode);
        sesMailSender.sendMail(mailRequest);
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
}
