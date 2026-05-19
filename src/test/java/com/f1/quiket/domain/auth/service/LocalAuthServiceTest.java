package com.f1.quiket.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.AuthUserResponse;
import com.f1.quiket.domain.auth.dto.LoginRequest;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.repository.UserEmailVerificationRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.mail.service.SesMailSender;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class LocalAuthServiceTest {

    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private AuthTokenService authTokenService;
    private LocalAuthService localAuthService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        authTokenService = mock(AuthTokenService.class);

        localAuthService = new LocalAuthService(
                userRepository,
                userAuthIdentityRepository,
                mock(UserEmailVerificationRepository.class),
                new BCryptPasswordEncoder(),
                mock(EmailVerificationCodeGenerator.class),
                mock(MailTemplateFactory.class),
                mock(SesMailSender.class),
                authTokenService
        );
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
