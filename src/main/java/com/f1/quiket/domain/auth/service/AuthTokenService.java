package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.AuthUserResponse;
import com.f1.quiket.domain.auth.dto.LogoutRequest;
import com.f1.quiket.domain.auth.dto.RefreshTokenRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthTokenService {

    private static final int DEVICE_ID_MAX_LENGTH = 128;
    private static final int DEVICE_NAME_MAX_LENGTH = 100;
    private static final int USER_AGENT_MAX_LENGTH = 500;
    private static final int IP_ADDRESS_MAX_LENGTH = 45;

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHashGenerator tokenHashGenerator;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;

    public AuthTokenResponse issueTokens(User user, AuthTokenRequestContext context) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = refreshTokenGenerator.generate();
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(jwtProperties.getRefreshTokenExpiresInSeconds());

        UserRefreshToken savedRefreshToken = UserRefreshToken.create(
                user,
                tokenHashGenerator.hash(refreshToken),
                normalize(context.getDeviceId(), DEVICE_ID_MAX_LENGTH),
                normalize(context.getDeviceName(), DEVICE_NAME_MAX_LENGTH),
                normalize(context.getUserAgent(), USER_AGENT_MAX_LENGTH),
                normalize(context.getIpAddress(), IP_ADDRESS_MAX_LENGTH),
                issuedAt,
                expiresAt
        );
        userRefreshTokenRepository.save(savedRefreshToken);
        return buildTokenResponse(user, accessToken, refreshToken);
    }

    public AuthTokenResponse refresh(RefreshTokenRequest request, AuthTokenRequestContext context) {
        UserRefreshToken refreshToken = findRefreshToken(request.getRefreshToken());
        LocalDateTime now = LocalDateTime.now();
        validateRefreshToken(refreshToken, now);

        refreshToken.recordUsed(now);
        User user = refreshToken.getUser();
        validateUserCanUseToken(user);
        refreshToken.revoke(now);

        return issueTokens(user, context);
    }

    public void logout(String userPublicId, LogoutRequest request) {
        User user = findUserByPublicId(userPublicId);
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            revokeRefreshToken(user, request.getRefreshToken());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        userRefreshTokenRepository.findAllByUserAndRevokedAtIsNullAndDeletedAtIsNull(user)
                .forEach(refreshToken -> refreshToken.revoke(now));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(String userPublicId) {
        User user = findUserByPublicId(userPublicId);
        return AuthUserResponse.of(user, userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user));
    }

    private void revokeRefreshToken(User user, String refreshTokenValue) {
        UserRefreshToken refreshToken = findRefreshToken(refreshTokenValue);
        if (!refreshToken.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!refreshToken.isRevoked()) {
            refreshToken.revoke(LocalDateTime.now());
        }
    }

    private UserRefreshToken findRefreshToken(String refreshToken) {
        return userRefreshTokenRepository.findByTokenHashAndDeletedAtIsNull(tokenHashGenerator.hash(refreshToken))
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_TOKEN));
    }

    private User findUserByPublicId(String publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_TOKEN));
    }

    private void validateRefreshToken(UserRefreshToken refreshToken, LocalDateTime now) {
        if (refreshToken.isRevoked()) {
            throw new CustomException(ErrorCode.AUTH_REVOKED_REFRESH_TOKEN);
        }
        if (refreshToken.isExpired(now)) {
            refreshToken.revoke(now);
            throw new CustomException(ErrorCode.AUTH_EXPIRED_TOKEN);
        }
    }

    private void validateUserCanUseToken(User user) {
        if (user.isLocked()) {
            throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    private AuthTokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        AuthUserResponse userResponse = AuthUserResponse.of(
                user,
                userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user)
        );
        return AuthTokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                jwtProperties.getRefreshTokenExpiresInSeconds(),
                userResponse
        );
    }

    private String normalize(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() <= maxLength) {
            return trimmedValue;
        }
        return trimmedValue.substring(0, maxLength);
    }
}
