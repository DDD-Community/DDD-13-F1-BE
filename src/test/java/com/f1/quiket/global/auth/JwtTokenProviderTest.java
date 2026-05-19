package com.f1.quiket.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("quiket-test");
        jwtProperties.setSecret("test-jwt-secret-for-quiket-f1-authentication");
        jwtProperties.setAccessTokenExpiresInSeconds(3600L);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void getSubject_succeeds_when_access_token_valid() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");

        String accessToken = jwtTokenProvider.createAccessToken(user);

        assertThat(jwtTokenProvider.getSubject(accessToken)).isEqualTo(user.getPublicId());
    }

    @Test
    void getSubject_fails_when_access_token_tampered() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String tamperedToken = accessToken.substring(0, accessToken.length() - 1) + "x";

        assertThatThrownBy(() -> jwtTokenProvider.getSubject(tamperedToken))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    }
}
