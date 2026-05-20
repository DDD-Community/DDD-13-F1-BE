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
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequest;
import com.f1.quiket.domain.auth.dto.KakaoLoginRequest;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequest;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.kakao.client.KakaoApiClient;
import com.f1.quiket.infra.kakao.dto.KakaoUserInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class KakaoOAuthServiceTest {

    private KakaoApiClient kakaoApiClient;
    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private AuthTokenService authTokenService;
    private KakaoOAuthTemporaryTokenStore temporaryTokenStore;
    private KakaoOAuthService kakaoOAuthService;

    @BeforeEach
    void setUp() {
        kakaoApiClient = mock(KakaoApiClient.class);
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        authTokenService = mock(AuthTokenService.class);
        temporaryTokenStore = mock(KakaoOAuthTemporaryTokenStore.class);

        kakaoOAuthService = new KakaoOAuthService(
                kakaoApiClient,
                userRepository,
                userAuthIdentityRepository,
                new BCryptPasswordEncoder(),
                authTokenService,
                temporaryTokenStore
        );
    }

    @Test
    void login_succeeds_when_kakao_identity_exists() {
        User user = verifiedUser("user@example.com", "도토리");
        UserAuthIdentity kakaoIdentity = UserAuthIdentity.createOAuth(user, "kakao", "123456789", true);
        KakaoLoginRequest request = kakaoLoginRequest("kakao-access-token");

        when(kakaoApiClient.getUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("123456789", "user@example.com", "도토리"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.of(kakaoIdentity));
        when(authTokenService.issueTokens(eq(user), any(AuthTokenRequestContext.class)))
                .thenReturn(tokenResponse(user, List.of(kakaoIdentity)));

        KakaoOAuthLoginResult result = kakaoOAuthService.login(request, tokenRequestContext());

        assertThat(result.getStatus()).isEqualTo(KakaoOAuthLoginStatus.EXISTING_LOGIN);
        assertThat(result.getTokenResponse().getAccessToken()).isEqualTo("access-token");
        assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
        assertThat(kakaoIdentity.getLastLoginAt()).isNotNull();
    }

    @Test
    void login_creates_user_and_kakao_identity_when_new_user_has_valid_nickname() {
        KakaoLoginRequest request = kakaoLoginRequest("kakao-access-token");
        when(kakaoApiClient.getUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("123456789", "new@example.com", "카카오"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issueTokens(any(User.class), any(AuthTokenRequestContext.class)))
                .thenAnswer(invocation -> tokenResponse(invocation.getArgument(0), List.of()));

        KakaoOAuthLoginResult result = kakaoOAuthService.login(request, tokenRequestContext());

        assertThat(result.getStatus()).isEqualTo(KakaoOAuthLoginStatus.SIGNUP_LOGIN);
        assertThat(result.getTokenResponse().getUser().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getTokenResponse().getUser().isEmailVerified()).isTrue();

        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(userAuthIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("kakao");
        assertThat(identityCaptor.getValue().getProviderSubject()).isEqualTo("123456789");
        assertThat(identityCaptor.getValue().getPasswordHash()).isNull();
    }

    @Test
    void login_returns_account_link_required_when_same_email_user_exists() {
        User user = verifiedUser("user@example.com", "도토리");
        KakaoLoginRequest request = kakaoLoginRequest("kakao-access-token");
        when(kakaoApiClient.getUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("123456789", "user@example.com", "카카오"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(temporaryTokenStore.saveLink(any(KakaoOAuthLinkTokenPayload.class), eq(600L)))
                .thenReturn("link-token");

        KakaoOAuthLoginResult result = kakaoOAuthService.login(request, tokenRequestContext());

        assertThat(result.getStatus()).isEqualTo(KakaoOAuthLoginStatus.ACCOUNT_LINK_REQUIRED);
        assertThat(result.getAccountLinkRequiredResponse().getEmail()).isEqualTo("user@example.com");
        assertThat(result.getAccountLinkRequiredResponse().getProvider()).isEqualTo("kakao");
        assertThat(result.getAccountLinkRequiredResponse().getLinkToken()).isEqualTo("link-token");
    }

    @Test
    void login_returns_nickname_required_when_kakao_nickname_is_invalid() {
        KakaoLoginRequest request = kakaoLoginRequest("kakao-access-token");
        when(kakaoApiClient.getUserInfo("kakao-access-token"))
                .thenReturn(kakaoUserInfo("123456789", "new@example.com", "카카오123"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        when(temporaryTokenStore.saveSignup(any(KakaoOAuthSignupTokenPayload.class), eq(600L)))
                .thenReturn("signup-token");

        KakaoOAuthLoginResult result = kakaoOAuthService.login(request, tokenRequestContext());

        assertThat(result.getStatus()).isEqualTo(KakaoOAuthLoginStatus.NICKNAME_REQUIRED);
        assertThat(result.getNicknameRequiredResponse().getSignupToken()).isEqualTo("signup-token");
        assertThat(result.getNicknameRequiredResponse().getSuggestedNickname()).isEqualTo("카카오");
    }

    @Test
    void login_fails_when_kakao_email_is_missing() {
        KakaoLoginRequest request = kakaoLoginRequest("kakao-access-token");
        when(kakaoApiClient.getUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserInfo("123456789", null, null, null, "카카오"));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> kakaoOAuthService.login(request, tokenRequestContext()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_EMAIL_REQUIRED);
    }

    @Test
    void link_adds_kakao_identity_after_local_password_validation() {
        User user = verifiedUser("user@example.com", "도토리");
        UserAuthIdentity localIdentity = UserAuthIdentity.createLocal(
                user,
                new BCryptPasswordEncoder().encode("Password123!"),
                true
        );
        KakaoAccountLinkRequest request = kakaoAccountLinkRequest(
                "link-token",
                "user@example.com",
                "Password123!"
        );

        when(temporaryTokenStore.findLink("link-token"))
                .thenReturn(Optional.of(new KakaoOAuthLinkTokenPayload("123456789", "user@example.com")));
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(localIdentity));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "kakao"))
                .thenReturn(Optional.empty());
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issueTokens(eq(user), any(AuthTokenRequestContext.class)))
                .thenReturn(tokenResponse(user, List.of(localIdentity)));

        AuthTokenResponse response = kakaoOAuthService.link(request, tokenRequestContext());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        ArgumentCaptor<UserAuthIdentity> identityCaptor = ArgumentCaptor.forClass(UserAuthIdentity.class);
        verify(userAuthIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo("kakao");
        assertThat(identityCaptor.getValue().getProviderSubject()).isEqualTo("123456789");
        verify(temporaryTokenStore).deleteLink("link-token");
    }

    @Test
    void completeNickname_creates_user_from_signup_token() {
        KakaoNicknameRequest request = kakaoNicknameRequest("signup-token", "도토리");
        when(temporaryTokenStore.findSignup("signup-token"))
                .thenReturn(Optional.of(new KakaoOAuthSignupTokenPayload("123456789", "new@example.com", "카카오")));
        when(userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull("kakao", "123456789"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAuthIdentityRepository.save(any(UserAuthIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issueTokens(any(User.class), any(AuthTokenRequestContext.class)))
                .thenAnswer(invocation -> tokenResponse(invocation.getArgument(0), List.of()));

        AuthTokenResponse response = kakaoOAuthService.completeNickname(request, tokenRequestContext());

        assertThat(response.getUser().getEmail()).isEqualTo("new@example.com");
        assertThat(response.getUser().getNickname()).isEqualTo("도토리");
        verify(temporaryTokenStore).deleteSignup("signup-token");
    }

    private KakaoLoginRequest kakaoLoginRequest(String kakaoAccessToken) {
        KakaoLoginRequest request = new KakaoLoginRequest();
        ReflectionTestUtils.setField(request, "kakaoAccessToken", kakaoAccessToken);
        return request;
    }

    private KakaoAccountLinkRequest kakaoAccountLinkRequest(String linkToken, String email, String password) {
        KakaoAccountLinkRequest request = new KakaoAccountLinkRequest();
        ReflectionTestUtils.setField(request, "linkToken", linkToken);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "agreedToLink", true);
        return request;
    }

    private KakaoNicknameRequest kakaoNicknameRequest(String signupToken, String nickname) {
        KakaoNicknameRequest request = new KakaoNicknameRequest();
        ReflectionTestUtils.setField(request, "signupToken", signupToken);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private KakaoUserInfo kakaoUserInfo(String providerSubject, String email, String nickname) {
        return new KakaoUserInfo(providerSubject, email, true, true, nickname);
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
                .deviceName("Galaxy S24")
                .userAgent("Android")
                .ipAddress("127.0.0.1")
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
