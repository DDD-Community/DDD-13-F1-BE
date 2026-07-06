package com.f1.quiket.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.dto.AppleLoginRequest;
import com.f1.quiket.domain.auth.dto.AppleNicknameRequest;
import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.AuthUserResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.apple.client.AppleAuthApiClient;
import com.f1.quiket.infra.apple.client.AppleIdTokenVerifier;
import com.f1.quiket.infra.apple.dto.AppleUserInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AppleOAuthServiceTest {

    private AppleIdTokenVerifier appleIdTokenVerifier;
    private AppleAuthApiClient appleAuthApiClient;
    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private AuthTokenService authTokenService;
    private AppleOAuthTemporaryTokenStore temporaryTokenStore;
    private AppleOAuthService appleOAuthService;

    @BeforeEach
    void setUp() {
        appleIdTokenVerifier = mock(AppleIdTokenVerifier.class);
        appleAuthApiClient = mock(AppleAuthApiClient.class);
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        authTokenService = mock(AuthTokenService.class);
        temporaryTokenStore = mock(AppleOAuthTemporaryTokenStore.class);

        appleOAuthService = new AppleOAuthService(
                appleIdTokenVerifier,
                appleAuthApiClient,
                userRepository,
                userAuthIdentityRepository,
                new BCryptPasswordEncoder(),
                authTokenService,
                temporaryTokenStore
        );
    }

    @Test
    void login_succeeds_when_apple_identity_exists() {
        User user = verifiedUser("user@example.com", "도토리");
        UserAuthIdentity appleIdentity = UserAuthIdentity.createOAuth(user, "apple", "001234.abcdef", true);
        appleIdentity.updateOAuthRefreshToken("stored-refresh-token");

        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", "user@example.com"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.of(appleIdentity));
        when(authTokenService.issueTokens(eq(user), any(AuthTokenRequestContext.class)))
                .thenReturn(tokenResponse(user, List.of(appleIdentity)));

        AppleOAuthLoginResult result = appleOAuthService.login(
                loginRequest("identity-token", "auth-code", null, null),
                tokenRequestContext()
        );

        assertThat(result.getStatus()).isEqualTo(AppleOAuthLoginStatus.EXISTING_LOGIN);
        assertThat(result.getTokenResponse().getAccessToken()).isEqualTo("access-token");
        assertThat(appleIdentity.getLastLoginAt()).isNotNull();
        // refresh token 보유 시 재교환 없음
        verify(appleAuthApiClient, never()).exchangeAuthorizationCode(any());
    }

    @Test
    void login_exchanges_refresh_token_when_existing_identity_has_none() {
        User user = verifiedUser("user@example.com", "도토리");
        UserAuthIdentity appleIdentity = UserAuthIdentity.createOAuth(user, "apple", "001234.abcdef", true);

        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", "user@example.com"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.of(appleIdentity));
        when(appleAuthApiClient.exchangeAuthorizationCode("auth-code"))
                .thenReturn(Optional.of("new-refresh-token"));
        when(authTokenService.issueTokens(eq(user), any(AuthTokenRequestContext.class)))
                .thenReturn(tokenResponse(user, List.of(appleIdentity)));

        appleOAuthService.login(loginRequest("identity-token", "auth-code", null, null), tokenRequestContext());

        assertThat(appleIdentity.getOauthRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void login_signs_up_directly_when_full_name_is_valid_nickname() {
        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", "new@example.com"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        when(appleAuthApiClient.exchangeAuthorizationCode("auth-code"))
                .thenReturn(Optional.of("refresh-token"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issueTokens(any(User.class), any(AuthTokenRequestContext.class)))
                .thenAnswer(invocation -> tokenResponse(invocation.getArgument(0), List.of()));

        AppleOAuthLoginResult result = appleOAuthService.login(
                loginRequest("identity-token", "auth-code", "하동헌", true),
                tokenRequestContext()
        );

        assertThat(result.getStatus()).isEqualTo(AppleOAuthLoginStatus.SIGNUP_LOGIN);
        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(userAuthIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("apple");
        assertThat(identityCaptor.getValue().getProviderSubject()).isEqualTo("001234.abcdef");
        assertThat(identityCaptor.getValue().getOauthRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_requires_nickname_when_full_name_is_missing() {
        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", "new@example.com"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        when(appleAuthApiClient.exchangeAuthorizationCode(any())).thenReturn(Optional.empty());
        when(temporaryTokenStore.saveSignup(any(AppleOAuthSignupTokenPayload.class), anyLong()))
                .thenReturn("signup-token");

        AppleOAuthLoginResult result = appleOAuthService.login(
                loginRequest("identity-token", null, null, true),
                tokenRequestContext()
        );

        assertThat(result.getStatus()).isEqualTo(AppleOAuthLoginStatus.NICKNAME_REQUIRED);
        assertThat(result.getNicknameRequiredResponse().getSignupToken()).isEqualTo("signup-token");
        assertThat(result.getNicknameRequiredResponse().getProvider()).isEqualTo("apple");
    }

    @Test
    void login_requires_account_link_when_email_belongs_to_existing_user() {
        User existingUser = verifiedUser("user@example.com", "도토리");
        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", "user@example.com"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(appleAuthApiClient.exchangeAuthorizationCode(any())).thenReturn(Optional.empty());
        when(temporaryTokenStore.saveLink(any(AppleOAuthLinkTokenPayload.class), anyLong()))
                .thenReturn("link-token");

        AppleOAuthLoginResult result = appleOAuthService.login(
                loginRequest("identity-token", null, null, null),
                tokenRequestContext()
        );

        assertThat(result.getStatus()).isEqualTo(AppleOAuthLoginStatus.ACCOUNT_LINK_REQUIRED);
        assertThat(result.getAccountLinkRequiredResponse().getLinkToken()).isEqualTo("link-token");
        assertThat(result.getAccountLinkRequiredResponse().getProvider()).isEqualTo("apple");
    }

    @Test
    void login_throws_when_terms_not_agreed_for_new_user() {
        when(appleIdTokenVerifier.verify("identity-token"))
                .thenReturn(appleUserInfo("001234.abcdef", null));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.empty());
        when(appleAuthApiClient.exchangeAuthorizationCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appleOAuthService.login(
                loginRequest("identity-token", null, "하동헌", null),
                tokenRequestContext()
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void completeNickname_creates_user_with_signup_payload() {
        when(temporaryTokenStore.findSignup("signup-token"))
                .thenReturn(Optional.of(new AppleOAuthSignupTokenPayload(
                        "001234.abcdef",
                        "new@example.com",
                        null,
                        "refresh-token"
                )));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("apple", "001234.abcdef"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issueTokens(any(User.class), any(AuthTokenRequestContext.class)))
                .thenAnswer(invocation -> tokenResponse(invocation.getArgument(0), List.of()));

        AuthTokenResponse response = appleOAuthService.completeNickname(
                nicknameRequest("signup-token", "도토리장인"),
                tokenRequestContext()
        );

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(userAuthIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getOauthRefreshToken()).isEqualTo("refresh-token");
        verify(temporaryTokenStore).deleteSignup("signup-token");
    }

    @Test
    void completeNickname_throws_when_signup_token_invalid() {
        when(temporaryTokenStore.findSignup("expired-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appleOAuthService.completeNickname(
                nicknameRequest("expired-token", "도토리장인"),
                tokenRequestContext()
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_SIGNUP_TOKEN_INVALID));
    }

    private AppleLoginRequest loginRequest(
            String identityToken,
            String authorizationCode,
            String fullName,
            Boolean agreedToTerms
    ) {
        AppleLoginRequest request = new AppleLoginRequest();
        ReflectionTestUtils.setField(request, "identityToken", identityToken);
        ReflectionTestUtils.setField(request, "authorizationCode", authorizationCode);
        ReflectionTestUtils.setField(request, "fullName", fullName);
        ReflectionTestUtils.setField(request, "agreedToTerms", agreedToTerms);
        return request;
    }

    private AppleNicknameRequest nicknameRequest(String signupToken, String nickname) {
        AppleNicknameRequest request = new AppleNicknameRequest();
        ReflectionTestUtils.setField(request, "signupToken", signupToken);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private AppleUserInfo appleUserInfo(String providerSubject, String email) {
        return new AppleUserInfo(providerSubject, email, email != null ? Boolean.TRUE : null);
    }

    private User verifiedUser(String email, String nickname) {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", email, nickname);
        ReflectionTestUtils.setField(user, "id", 1L);
        user.verifyEmail();
        return user;
    }

    private AuthTokenRequestContext tokenRequestContext() {
        return AuthTokenRequestContext.builder()
                .deviceId("device-id")
                .deviceName("iPhone 16")
                .userAgent("iOS")
                .ipAddress("127.0.0.1")
                .build();
    }

    private AuthTokenResponse tokenResponse(User user, List<UserAuthIdentity> identities) {
        return AuthTokenResponse.of(
                "access-token",
                "refresh-token",
                1800L,
                2592000L,
                AuthUserResponse.of(user, identities)
        );
    }
}
