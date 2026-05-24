package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequest;
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequiredResponse;
import com.f1.quiket.domain.auth.dto.KakaoLoginRequest;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequest;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequiredResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
import com.f1.quiket.infra.kakao.client.KakaoApiClient;
import com.f1.quiket.infra.kakao.dto.KakaoUserInfo;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoOAuthService {

    private static final String PROVIDER_KAKAO = "kakao";
    private static final String PROVIDER_LOCAL = "local";
    private static final long KAKAO_TEMP_TOKEN_TTL_SECONDS = 600L;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z]{2,12}$");

    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final KakaoOAuthTemporaryTokenStore temporaryTokenStore;

    public KakaoOAuthLoginResult login(KakaoLoginRequest request, AuthTokenRequestContext context) {
        KakaoUserInfo kakaoUserInfo = kakaoApiClient.getUserInfo(request.getKakaoAccessToken());
        UserAuthIdentity kakaoIdentity = userAuthIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(
                        PROVIDER_KAKAO,
                        kakaoUserInfo.providerSubject()
                )
                .orElse(null);

        if (kakaoIdentity != null) {
            return loginWithExistingKakaoIdentity(kakaoIdentity, context);
        }

        validateUsableKakaoEmail(kakaoUserInfo);
        return loginWithNewKakaoIdentity(kakaoUserInfo, request.getAgreedToTerms(), context);
    }

    public AuthTokenResponse link(KakaoAccountLinkRequest request, AuthTokenRequestContext context) {
        KakaoOAuthLinkTokenPayload payload = temporaryTokenStore.findLink(request.getLinkToken())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_OAUTH_LINK_TOKEN_INVALID));

        if (!payload.email().equals(request.getEmail())) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_LINK_TOKEN_INVALID);
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
        validateAccountIsNotLocked(user);

        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));
        validatePassword(localIdentity, request.getPassword());

        UserAuthIdentity existingKakaoIdentity = userAuthIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_KAKAO, payload.providerSubject())
                .orElse(null);
        if (existingKakaoIdentity != null && !existingKakaoIdentity.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_PROVIDER_ALREADY_LINKED);
        }

        UserAuthIdentity userKakaoIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_KAKAO)
                .orElse(null);
        if (userKakaoIdentity != null
                && !userKakaoIdentity.getProviderSubject().equals(payload.providerSubject())) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_ACCOUNT_ALREADY_LINKED);
        }

        UserAuthIdentity kakaoIdentity = userKakaoIdentity == null
                ? userAuthIdentityRepository.save(UserAuthIdentity.createOAuth(
                        user,
                        PROVIDER_KAKAO,
                        payload.providerSubject(),
                        false
                ))
                : userKakaoIdentity;

        if (!user.isEmailVerified()) {
            user.verifyEmail();
        }
        recordLoginSuccess(user, kakaoIdentity, context);
        temporaryTokenStore.deleteLink(request.getLinkToken());
        return authTokenService.issueTokens(user, context);
    }

    public AuthTokenResponse completeNickname(KakaoNicknameRequest request, AuthTokenRequestContext context) {
        if (!isValidNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        KakaoOAuthSignupTokenPayload payload = temporaryTokenStore.findSignup(request.getSignupToken())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_OAUTH_SIGNUP_TOKEN_INVALID));

        if (userAuthIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(
                PROVIDER_KAKAO,
                payload.providerSubject()
        ).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_PROVIDER_ALREADY_LINKED);
        }
        if (userRepository.existsByEmail(payload.email())) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        AuthTokenResponse response = createKakaoUserAndIssueTokens(
                payload.providerSubject(),
                payload.email(),
                request.getNickname(),
                context
        );
        temporaryTokenStore.deleteSignup(request.getSignupToken());
        return response;
    }

    private KakaoOAuthLoginResult loginWithExistingKakaoIdentity(
            UserAuthIdentity kakaoIdentity,
            AuthTokenRequestContext context
    ) {
        User user = kakaoIdentity.getUser();
        validateAccountIsNotLocked(user);
        recordLoginSuccess(user, kakaoIdentity, context);
        return KakaoOAuthLoginResult.existingLogin(authTokenService.issueTokens(user, context));
    }

    private KakaoOAuthLoginResult loginWithNewKakaoIdentity(
            KakaoUserInfo kakaoUserInfo,
            Boolean agreedToTerms,
            AuthTokenRequestContext context
    ) {
        User existingUser = userRepository.findByEmailAndDeletedAtIsNull(kakaoUserInfo.email())
                .orElse(null);
        if (existingUser != null) {
            String linkToken = temporaryTokenStore.saveLink(
                    new KakaoOAuthLinkTokenPayload(kakaoUserInfo.providerSubject(), kakaoUserInfo.email()),
                    KAKAO_TEMP_TOKEN_TTL_SECONDS
            );
            return KakaoOAuthLoginResult.accountLinkRequired(
                    KakaoAccountLinkRequiredResponse.of(kakaoUserInfo.email(), linkToken, KAKAO_TEMP_TOKEN_TTL_SECONDS)
            );
        }

        validateTermsAgreed(agreedToTerms);
        if (!isValidNickname(kakaoUserInfo.nickname())) {
            String suggestedNickname = resolveSuggestedNickname(kakaoUserInfo.nickname());
            String signupToken = temporaryTokenStore.saveSignup(
                    new KakaoOAuthSignupTokenPayload(
                            kakaoUserInfo.providerSubject(),
                            kakaoUserInfo.email(),
                            suggestedNickname
                    ),
                    KAKAO_TEMP_TOKEN_TTL_SECONDS
            );
            return KakaoOAuthLoginResult.nicknameRequired(
                    KakaoNicknameRequiredResponse.of(signupToken, suggestedNickname)
            );
        }

        return KakaoOAuthLoginResult.signupLogin(createKakaoUserAndIssueTokens(
                kakaoUserInfo.providerSubject(),
                kakaoUserInfo.email(),
                kakaoUserInfo.nickname(),
                context
        ));
    }

    private AuthTokenResponse createKakaoUserAndIssueTokens(
            String providerSubject,
            String email,
            String nickname,
            AuthTokenRequestContext context
    ) {
        User user = User.create(UuidV7Generator.generate(), email, nickname);
        user.verifyEmail();
        User savedUser = userRepository.save(user);

        UserAuthIdentity kakaoIdentity = UserAuthIdentity.createOAuth(
                savedUser,
                PROVIDER_KAKAO,
                providerSubject,
                true
        );
        userAuthIdentityRepository.save(kakaoIdentity);

        recordLoginSuccess(savedUser, kakaoIdentity, context);
        return authTokenService.issueTokens(savedUser, context);
    }

    private void validateUsableKakaoEmail(KakaoUserInfo kakaoUserInfo) {
        if (!kakaoUserInfo.hasUsableEmail()) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_EMAIL_REQUIRED);
        }
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

    private void recordLoginSuccess(User user, UserAuthIdentity kakaoIdentity, AuthTokenRequestContext context) {
        user.recordLoginSuccess(context.getIpAddress());
        kakaoIdentity.recordLoginSuccess();
    }

    private boolean isValidNickname(String nickname) {
        return StringUtils.hasText(nickname) && NICKNAME_PATTERN.matcher(nickname).matches();
    }

    private String resolveSuggestedNickname(String kakaoNickname) {
        if (!StringUtils.hasText(kakaoNickname)) {
            return null;
        }

        String suggestedNickname = kakaoNickname.replaceAll("[^가-힣A-Za-z]", "");
        if (suggestedNickname.length() > 12) {
            suggestedNickname = suggestedNickname.substring(0, 12);
        }
        if (!isValidNickname(suggestedNickname)) {
            return null;
        }
        return suggestedNickname;
    }
}
