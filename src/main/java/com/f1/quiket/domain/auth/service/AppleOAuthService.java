package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AppleAccountLinkRequest;
import com.f1.quiket.domain.auth.dto.AppleAccountLinkRequiredResponse;
import com.f1.quiket.domain.auth.dto.AppleLoginRequest;
import com.f1.quiket.domain.auth.dto.AppleNicknameRequest;
import com.f1.quiket.domain.auth.dto.AppleNicknameRequiredResponse;
import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
import com.f1.quiket.infra.apple.client.AppleAuthApiClient;
import com.f1.quiket.infra.apple.client.AppleIdTokenVerifier;
import com.f1.quiket.infra.apple.dto.AppleUserInfo;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AppleOAuthService {

    private static final String PROVIDER_APPLE = "apple";
    private static final String PROVIDER_LOCAL = "local";
    private static final long APPLE_TEMP_TOKEN_TTL_SECONDS = 600L;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z]{2,12}$");

    private final AppleIdTokenVerifier appleIdTokenVerifier;
    private final AppleAuthApiClient appleAuthApiClient;
    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final AppleOAuthTemporaryTokenStore temporaryTokenStore;

    public AppleOAuthLoginResult login(AppleLoginRequest request, AuthTokenRequestContext context) {
        AppleUserInfo appleUserInfo = appleIdTokenVerifier.verify(request.getIdentityToken());
        UserAuthIdentity appleIdentity = userAuthIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(
                        PROVIDER_APPLE,
                        appleUserInfo.providerSubject()
                )
                .orElse(null);

        if (appleIdentity != null) {
            return loginWithExistingAppleIdentity(appleIdentity, request.getAuthorizationCode(), context);
        }

        return loginWithNewAppleIdentity(appleUserInfo, request, context);
    }

    public AuthTokenResponse link(AppleAccountLinkRequest request, AuthTokenRequestContext context) {
        AppleOAuthLinkTokenPayload payload = temporaryTokenStore.findLink(request.getLinkToken())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_APPLE_LINK_TOKEN_INVALID));

        if (!payload.email().equals(request.getEmail())) {
            throw new CustomException(ErrorCode.AUTH_APPLE_LINK_TOKEN_INVALID);
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
        validateAccountIsNotLocked(user);

        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));
        validatePassword(localIdentity, request.getPassword());

        UserAuthIdentity existingAppleIdentity = userAuthIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_APPLE, payload.providerSubject())
                .orElse(null);
        if (existingAppleIdentity != null && !existingAppleIdentity.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.AUTH_APPLE_PROVIDER_ALREADY_LINKED);
        }

        UserAuthIdentity userAppleIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_APPLE)
                .orElse(null);
        if (userAppleIdentity != null
                && !userAppleIdentity.getProviderSubject().equals(payload.providerSubject())) {
            throw new CustomException(ErrorCode.AUTH_APPLE_ACCOUNT_ALREADY_LINKED);
        }

        UserAuthIdentity appleIdentity = userAppleIdentity == null
                ? userAuthIdentityRepository.save(UserAuthIdentity.createOAuth(
                        user,
                        PROVIDER_APPLE,
                        payload.providerSubject(),
                        false
                ))
                : userAppleIdentity;
        applyOAuthRefreshToken(appleIdentity, payload.oauthRefreshToken());

        if (!user.isEmailVerified()) {
            user.verifyEmail();
        }
        recordLoginSuccess(user, appleIdentity, context);
        temporaryTokenStore.deleteLink(request.getLinkToken());
        return authTokenService.issueTokens(user, context);
    }

    public AuthTokenResponse completeNickname(AppleNicknameRequest request, AuthTokenRequestContext context) {
        if (!isValidNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        AppleOAuthSignupTokenPayload payload = temporaryTokenStore.findSignup(request.getSignupToken())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_APPLE_SIGNUP_TOKEN_INVALID));

        if (userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(
                PROVIDER_APPLE,
                payload.providerSubject()
        ).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_APPLE_PROVIDER_ALREADY_LINKED);
        }
        if (StringUtils.hasText(payload.email()) && userRepository.existsByEmail(payload.email())) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        AuthTokenResponse response = createAppleUserAndIssueTokens(
                payload.providerSubject(),
                payload.email(),
                request.getNickname(),
                payload.oauthRefreshToken(),
                context
        );
        temporaryTokenStore.deleteSignup(request.getSignupToken());
        return response;
    }

    private AppleOAuthLoginResult loginWithExistingAppleIdentity(
            UserAuthIdentity appleIdentity,
            String authorizationCode,
            AuthTokenRequestContext context
    ) {
        User user = appleIdentity.getUser();
        validateAccountIsNotLocked(user);
        if (!StringUtils.hasText(appleIdentity.getOauthRefreshToken())) {
            // 탈퇴 revoke용 refresh token 미보유 시 재확보
            applyOAuthRefreshToken(appleIdentity, exchangeRefreshToken(authorizationCode));
        }
        recordLoginSuccess(user, appleIdentity, context);
        return AppleOAuthLoginResult.existingLogin(authTokenService.issueTokens(user, context));
    }

    private AppleOAuthLoginResult loginWithNewAppleIdentity(
            AppleUserInfo appleUserInfo,
            AppleLoginRequest request,
            AuthTokenRequestContext context
    ) {
        String email = resolveUsableEmail(appleUserInfo);
        String oauthRefreshToken = exchangeRefreshToken(request.getAuthorizationCode());

        if (email != null) {
            User existingUser = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
            if (existingUser != null) {
                String linkToken = temporaryTokenStore.saveLink(
                        new AppleOAuthLinkTokenPayload(
                                appleUserInfo.providerSubject(),
                                email,
                                oauthRefreshToken
                        ),
                        APPLE_TEMP_TOKEN_TTL_SECONDS
                );
                return AppleOAuthLoginResult.accountLinkRequired(
                        AppleAccountLinkRequiredResponse.of(email, linkToken, APPLE_TEMP_TOKEN_TTL_SECONDS)
                );
            }
        }

        validateTermsAgreed(request.getAgreedToTerms());
        String nicknameCandidate = request.getFullName();
        if (!isValidNickname(nicknameCandidate)) {
            String suggestedNickname = resolveSuggestedNickname(nicknameCandidate);
            String signupToken = temporaryTokenStore.saveSignup(
                    new AppleOAuthSignupTokenPayload(
                            appleUserInfo.providerSubject(),
                            email,
                            suggestedNickname,
                            oauthRefreshToken
                    ),
                    APPLE_TEMP_TOKEN_TTL_SECONDS
            );
            return AppleOAuthLoginResult.nicknameRequired(
                    AppleNicknameRequiredResponse.of(signupToken, suggestedNickname)
            );
        }

        return AppleOAuthLoginResult.signupLogin(createAppleUserAndIssueTokens(
                appleUserInfo.providerSubject(),
                email,
                nicknameCandidate,
                oauthRefreshToken,
                context
        ));
    }

    private AuthTokenResponse createAppleUserAndIssueTokens(
            String providerSubject,
            String email,
            String nickname,
            String oauthRefreshToken,
            AuthTokenRequestContext context
    ) {
        User user = User.create(UuidV7Generator.generate(), email, nickname);
        if (StringUtils.hasText(email)) {
            user.verifyEmail();
        }
        User savedUser = userRepository.save(user);

        UserAuthIdentity appleIdentity = UserAuthIdentity.createOAuth(
                savedUser,
                PROVIDER_APPLE,
                providerSubject,
                true
        );
        applyOAuthRefreshToken(appleIdentity, oauthRefreshToken);
        userAuthIdentityRepository.save(appleIdentity);

        recordLoginSuccess(savedUser, appleIdentity, context);
        return authTokenService.issueTokens(savedUser, context);
    }

    private String exchangeRefreshToken(String authorizationCode) {
        return appleAuthApiClient.exchangeAuthorizationCode(authorizationCode).orElse(null);
    }

    private void applyOAuthRefreshToken(UserAuthIdentity appleIdentity, String oauthRefreshToken) {
        if (StringUtils.hasText(oauthRefreshToken)) {
            appleIdentity.updateOAuthRefreshToken(oauthRefreshToken);
        }
    }

    private String resolveUsableEmail(AppleUserInfo appleUserInfo) {
        // 검증된 이메일만 저장·연동 대상, 미제공 시 소셜 전용 계정 처리 위해 null 반환
        return appleUserInfo.hasUsableEmail() ? appleUserInfo.email() : null;
    }

    private void validateTermsAgreed(Boolean agreedToTerms) {
        if (!Boolean.TRUE.equals(agreedToTerms)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateAccountIsNotLocked(User user) {
        if (user.isLocked()) {
            throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    private void validatePassword(UserAuthIdentity localIdentity, String password) {
        if (!StringUtils.hasText(localIdentity.getPasswordHash())
                || !passwordEncoder.matches(password, localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private void recordLoginSuccess(User user, UserAuthIdentity appleIdentity, AuthTokenRequestContext context) {
        user.recordLoginSuccess(context.getIpAddress());
        appleIdentity.recordLoginSuccess();
    }

    private boolean isValidNickname(String nickname) {
        return StringUtils.hasText(nickname) && NICKNAME_PATTERN.matcher(nickname).matches();
    }

    private String resolveSuggestedNickname(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return null;
        }

        String suggestedNickname = fullName.replaceAll("[^가-힣A-Za-z]", "");
        if (suggestedNickname.length() > 12) {
            suggestedNickname = suggestedNickname.substring(0, 12);
        }
        if (!isValidNickname(suggestedNickname)) {
            return null;
        }
        return suggestedNickname;
    }
}
