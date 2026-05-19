package com.f1.quiket.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.LogoutRequest;
import com.f1.quiket.domain.auth.dto.RefreshTokenRequest;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.entity.UserRefreshToken;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.repository.UserRefreshTokenRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.auth.JwtProperties;
import com.f1.quiket.global.auth.JwtTokenProvider;
import com.f1.quiket.global.auth.RefreshTokenGenerator;
import com.f1.quiket.global.auth.TokenHashGenerator;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthTokenServiceTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private RefreshTokenGenerator refreshTokenGenerator;
    private TokenHashGenerator tokenHashGenerator;
    private UserRefreshTokenRepository userRefreshTokenRepository;
    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiresInSeconds(1209600L);
        refreshTokenGenerator = mock(RefreshTokenGenerator.class);
        tokenHashGenerator = new TokenHashGenerator();
        userRefreshTokenRepository = mock(UserRefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);

        authTokenService = new AuthTokenService(
                jwtTokenProvider,
                jwtProperties,
                refreshTokenGenerator,
                tokenHashGenerator,
                userRefreshTokenRepository,
                userRepository,
                userAuthIdentityRepository
        );
    }

    @Test
    void refresh_succeeds_when_refresh_token_valid() {
        User user = verifiedUser();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "hash", true);
        String refreshTokenValue = "refresh-token";
        UserRefreshToken refreshToken = refreshToken(user, refreshTokenValue, LocalDateTime.now().plusDays(14));
        RefreshTokenRequest request = refreshTokenRequest(refreshTokenValue);

        when(userRefreshTokenRepository.findByTokenHashAndDeletedAtIsNull(tokenHashGenerator.hash(refreshTokenValue)))
                .thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L);
        when(refreshTokenGenerator.generate()).thenReturn("new-refresh-token");
        when(userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user)).thenReturn(List.of(identity));

        AuthTokenResponse response = authTokenService.refresh(request, tokenRequestContext());

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(refreshToken.getLastUsedAt()).isNotNull();
        assertThat(refreshToken.isRevoked()).isTrue();
    }

    @Test
    void refresh_fails_when_refresh_token_revoked() {
        User user = verifiedUser();
        String refreshTokenValue = "refresh-token";
        UserRefreshToken refreshToken = refreshToken(user, refreshTokenValue, LocalDateTime.now().plusDays(14));
        refreshToken.revoke(LocalDateTime.now());

        when(userRefreshTokenRepository.findByTokenHashAndDeletedAtIsNull(tokenHashGenerator.hash(refreshTokenValue)))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authTokenService.refresh(refreshTokenRequest(refreshTokenValue), tokenRequestContext()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_REVOKED_REFRESH_TOKEN);
    }

    @Test
    void logout_revokes_requested_refresh_token() {
        User user = verifiedUser();
        String refreshTokenValue = "refresh-token";
        UserRefreshToken refreshToken = refreshToken(user, refreshTokenValue, LocalDateTime.now().plusDays(14));
        LogoutRequest request = logoutRequest(refreshTokenValue);

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userRefreshTokenRepository.findByTokenHashAndDeletedAtIsNull(tokenHashGenerator.hash(refreshTokenValue)))
                .thenReturn(Optional.of(refreshToken));

        authTokenService.logout(user.getPublicId(), request);

        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isNotNull();
    }

    @Test
    void logout_revokes_all_active_refresh_tokens_when_refresh_token_missing() {
        User user = verifiedUser();
        UserRefreshToken firstToken = refreshToken(user, "first-refresh-token", LocalDateTime.now().plusDays(14));
        UserRefreshToken secondToken = refreshToken(user, "second-refresh-token", LocalDateTime.now().plusDays(14));

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userRefreshTokenRepository.findAllByUserAndRevokedAtIsNullAndDeletedAtIsNull(user))
                .thenReturn(List.of(firstToken, secondToken));

        authTokenService.logout(user.getPublicId(), null);

        assertThat(firstToken.isRevoked()).isTrue();
        assertThat(secondToken.isRevoked()).isTrue();
    }

    private User verifiedUser() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.verifyEmail();
        return user;
    }

    private UserRefreshToken refreshToken(User user, String refreshTokenValue, LocalDateTime expiresAt) {
        return UserRefreshToken.create(
                user,
                tokenHashGenerator.hash(refreshTokenValue),
                "device-id",
                "Galaxy S24",
                "Android",
                "127.0.0.1",
                LocalDateTime.now(),
                expiresAt
        );
    }

    private RefreshTokenRequest refreshTokenRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }

    private AuthTokenRequestContext tokenRequestContext() {
        return AuthTokenRequestContext.builder()
                .deviceId("device-id")
                .deviceName("Galaxy S24")
                .userAgent("Android")
                .ipAddress("127.0.0.1")
                .build();
    }

    private LogoutRequest logoutRequest(String refreshToken) {
        LogoutRequest request = new LogoutRequest();
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }
}
