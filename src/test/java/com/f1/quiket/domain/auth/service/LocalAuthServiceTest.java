package com.f1.quiket.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.AuthUserResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class LocalAuthServiceTest {

    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private UserEmailVerificationRepository userEmailVerificationRepository;
    private EmailVerificationCodeGenerator verificationCodeGenerator;
    private AuthTokenService authTokenService;
    private ApplicationEventPublisher eventPublisher;
    private LocalAuthService localAuthService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        userEmailVerificationRepository = mock(UserEmailVerificationRepository.class);
        verificationCodeGenerator = mock(EmailVerificationCodeGenerator.class);
        authTokenService = mock(AuthTokenService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        localAuthService = new LocalAuthService(
                userRepository,
                userAuthIdentityRepository,
                userEmailVerificationRepository,
                new BCryptPasswordEncoder(),
                verificationCodeGenerator,
                authTokenService,
                eventPublisher
        );
    }

    @Test
    void signup_uses_user_public_id_as_local_provider_subject() {
        SignupRequest request = signupRequest("new@example.com", "Password123!", "도토리");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationCodeGenerator.generate()).thenReturn("123456");
        when(userEmailVerificationRepository.save(any(UserEmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = localAuthService.signup(request);

        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(userAuthIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProviderSubject()).isEqualTo(response.getUserId());

        ArgumentCaptor<EmailVerificationMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(EmailVerificationMailRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("new@example.com");
        assertThat(eventCaptor.getValue().verificationCode()).isEqualTo("123456");
    }

    @Test
    void resendEmailVerification_returns_ten_minute_ttl() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeGenerator.generate()).thenReturn("123456");
        when(userEmailVerificationRepository.save(any(UserEmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationSentResponse response = localAuthService.resendEmailVerification("user@example.com");

        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);
        verify(eventPublisher).publishEvent(any(EmailVerificationMailRequestedEvent.class));
    }

    @Test
    void expirePendingEmailVerifications_updates_expired_pending_rows() {
        when(userEmailVerificationRepository.expirePendingVerifications(
                eq("pending"),
                eq("expired"),
                any(LocalDateTime.class)
        )).thenReturn(3);

        int expiredCount = localAuthService.expirePendingEmailVerifications();

        assertThat(expiredCount).isEqualTo(3);
    }

    @Test
    void login_succeeds_when_local_user_verified() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        user.verifyEmail();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(
                user,
                new BCryptPasswordEncoder().encode("Password123!"),
                true
        );
        LoginRequest request = loginRequest("user@example.com", "Password123!");

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(authTokenService.issueTokens(any(User.class), any(AuthTokenRequestContext.class)))
                .thenReturn(tokenResponse(user, List.of(identity)));

        AuthTokenResponse response = localAuthService.login(request, tokenRequestContext("127.0.0.1"));

        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(identity.getLastLoginAt()).isNotNull();
    }

    @Test
    void login_locks_account_after_five_failures() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        user.verifyEmail();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(
                user,
                new BCryptPasswordEncoder().encode("Password123!"),
                true
        );
        LoginRequest request = loginRequest("user@example.com", "WrongPassword123!");

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> localAuthService.login(request, tokenRequestContext("127.0.0.1")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        assertThatThrownBy(() -> localAuthService.login(request, tokenRequestContext("127.0.0.1")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED);
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedAt()).isNotNull();
    }

    @Test
    void login_fails_when_email_not_verified() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        UserAuthIdentity identity = UserAuthIdentity.createLocal(
                user,
                new BCryptPasswordEncoder().encode("Password123!"),
                true
        );
        LoginRequest request = loginRequest("user@example.com", "Password123!");

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));

        assertThatThrownBy(() -> localAuthService.login(request, tokenRequestContext("127.0.0.1")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private SignupRequest signupRequest(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "passwordConfirm", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private AuthTokenRequestContext tokenRequestContext(String ipAddress) {
        return AuthTokenRequestContext.builder()
                .ipAddress(ipAddress)
                .build();
    }

    private AuthTokenResponse tokenResponse(User user, List<UserAuthIdentity> identities) {
        return AuthTokenResponse.of(
                "access-token",
                "refresh-token",
                3600L,
                1209600L,
                AuthUserResponse.of(user, identities)
        );
    }
}
