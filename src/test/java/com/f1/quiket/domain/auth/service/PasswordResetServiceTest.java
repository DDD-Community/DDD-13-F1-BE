package com.f1.quiket.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private UserPasswordResetTokenRepository userPasswordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationCodeGenerator verificationCodeGenerator;
    private AuthTokenService authTokenService;
    private ApplicationEventPublisher eventPublisher;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        userPasswordResetTokenRepository = mock(UserPasswordResetTokenRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        verificationCodeGenerator = mock(EmailVerificationCodeGenerator.class);
        authTokenService = mock(AuthTokenService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        passwordResetService = new PasswordResetService(
                userRepository,
                userAuthIdentityRepository,
                userPasswordResetTokenRepository,
                passwordEncoder,
                verificationCodeGenerator,
                authTokenService,
                eventPublisher
        );
    }

    @Test
    void requestPasswordReset_cancels_previous_token_and_sends_mail() {
        User user = verifiedUser();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, passwordEncoder.encode("Password123!"), true);
        PasswordResetRequest request = passwordResetRequest("user@example.com");

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(verificationCodeGenerator.generate()).thenReturn("123456");
        when(userPasswordResetTokenRepository.save(any(UserPasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetRequestedResponse response = passwordResetService.requestPasswordReset(
                request,
                tokenRequestContext("127.0.0.1")
        );

        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);
        verify(userPasswordResetTokenRepository).cancelPendingResetTokens(user, "pending", "cancelled");

        ArgumentCaptor<UserPasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(UserPasswordResetToken.class);
        verify(userPasswordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getVerificationCode()).isEqualTo("123456");
        assertThat(tokenCaptor.getValue().getRequestedIp()).isEqualTo("127.0.0.1");

        ArgumentCaptor<PasswordResetMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PasswordResetMailRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("user@example.com");
        assertThat(eventCaptor.getValue().verificationCode()).isEqualTo("123456");
    }

    @Test
    void requestPasswordReset_fails_when_social_only_account() {
        User user = verifiedUser();
        PasswordResetRequest request = passwordResetRequest("user@example.com");

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(request, tokenRequestContext("127.0.0.1")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND);
    }

    @Test
    void confirmPasswordReset_changes_password_unlocks_user_and_revokes_tokens() {
        User user = verifiedUser();
        user.recordLoginFailure(1);
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, passwordEncoder.encode("Password123!"), true);
        UserPasswordResetToken resetToken = UserPasswordResetToken.create(
                user,
                "reset-token",
                "123456",
                "127.0.0.1",
                LocalDateTime.now().plusMinutes(10)
        );
        PasswordResetConfirmRequest request = passwordResetConfirmRequest(
                "user@example.com",
                "123456",
                "NewPassword123!"
        );

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userPasswordResetTokenRepository
                .findTopByUserAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        user,
                        "123456",
                        "pending"
                )).thenReturn(Optional.of(resetToken));

        passwordResetService.confirmPasswordReset(request);

        assertThat(passwordEncoder.matches("NewPassword123!", identity.getPasswordHash())).isTrue();
        assertThat(resetToken.getStatus()).isEqualTo("used");
        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginCount()).isZero();
        verify(authTokenService).revokeAllActiveRefreshTokens(user);
    }

    @Test
    void confirmPasswordReset_fails_when_same_as_previous_password() {
        User user = verifiedUser();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, passwordEncoder.encode("Password123!"), true);
        UserPasswordResetToken resetToken = UserPasswordResetToken.create(
                user,
                "reset-token",
                "123456",
                "127.0.0.1",
                LocalDateTime.now().plusMinutes(10)
        );
        PasswordResetConfirmRequest request = passwordResetConfirmRequest(
                "user@example.com",
                "123456",
                "Password123!"
        );

        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userPasswordResetTokenRepository
                .findTopByUserAndVerificationCodeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        user,
                        "123456",
                        "pending"
                )).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_PASSWORD_SAME_AS_PREVIOUS);
    }

    private User verifiedUser() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.verifyEmail();
        return user;
    }

    private AuthTokenRequestContext tokenRequestContext(String ipAddress) {
        return AuthTokenRequestContext.builder()
                .ipAddress(ipAddress)
                .build();
    }

    private PasswordResetRequest passwordResetRequest(String email) {
        PasswordResetRequest request = new PasswordResetRequest();
        ReflectionTestUtils.setField(request, "email", email);
        return request;
    }

    private PasswordResetConfirmRequest passwordResetConfirmRequest(
            String email,
            String verificationCode,
            String newPassword
    ) {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "verificationCode", verificationCode);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);
        ReflectionTestUtils.setField(request, "newPasswordConfirm", newPassword);
        return request;
    }
}
