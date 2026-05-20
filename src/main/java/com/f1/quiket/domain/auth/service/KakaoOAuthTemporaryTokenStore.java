package com.f1.quiket.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Kakao OAuth 임시 토큰 저장소
 */
@Component
@RequiredArgsConstructor
public class KakaoOAuthTemporaryTokenStore {

    private static final String SIGNUP_TOKEN_PREFIX = "auth:kakao:signup:";
    private static final String LINK_TOKEN_PREFIX = "auth:kakao:link:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String saveSignup(KakaoOAuthSignupTokenPayload payload, long ttlSeconds) {
        return save(SIGNUP_TOKEN_PREFIX, payload, ttlSeconds);
    }

    public Optional<KakaoOAuthSignupTokenPayload> findSignup(String signupToken) {
        return find(SIGNUP_TOKEN_PREFIX, signupToken, KakaoOAuthSignupTokenPayload.class);
    }

    public void deleteSignup(String signupToken) {
        delete(SIGNUP_TOKEN_PREFIX, signupToken);
    }

    public String saveLink(KakaoOAuthLinkTokenPayload payload, long ttlSeconds) {
        return save(LINK_TOKEN_PREFIX, payload, ttlSeconds);
    }

    public Optional<KakaoOAuthLinkTokenPayload> findLink(String linkToken) {
        return find(LINK_TOKEN_PREFIX, linkToken, KakaoOAuthLinkTokenPayload.class);
    }

    public void deleteLink(String linkToken) {
        delete(LINK_TOKEN_PREFIX, linkToken);
    }

    private String save(String prefix, Object payload, long ttlSeconds) {
        try {
            String token = UUID.randomUUID().toString();
            stringRedisTemplate.opsForValue()
                    .set(prefix + token, objectMapper.writeValueAsString(payload), Duration.ofSeconds(ttlSeconds));
            return token;
        } catch (DataAccessException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_TEMP_TOKEN_STORE_FAILED);
        }
    }

    private <T> Optional<T> find(String prefix, String token, Class<T> payloadType) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            String payload = stringRedisTemplate.opsForValue().get(prefix + token);
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, payloadType));
        } catch (DataAccessException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_TEMP_TOKEN_STORE_FAILED);
        }
    }

    private void delete(String prefix, String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            stringRedisTemplate.delete(prefix + token);
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_TEMP_TOKEN_STORE_FAILED);
        }
    }
}
