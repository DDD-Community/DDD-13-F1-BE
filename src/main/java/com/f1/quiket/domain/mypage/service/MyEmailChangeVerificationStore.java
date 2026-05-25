package com.f1.quiket.domain.mypage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MyEmailChangeVerificationStore {

    private static final String KEY_PREFIX = "my:email-change:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(String userPublicId, MyEmailChangeVerificationPayload payload, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(
                            buildKey(userPublicId),
                            objectMapper.writeValueAsString(payload),
                            Duration.ofSeconds(ttlSeconds)
                    );
        } catch (DataAccessException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    public Optional<MyEmailChangeVerificationPayload> find(String userPublicId) {
        try {
            String payload = stringRedisTemplate.opsForValue().get(buildKey(userPublicId));
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, MyEmailChangeVerificationPayload.class));
        } catch (DataAccessException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    public void delete(String userPublicId) {
        try {
            stringRedisTemplate.delete(buildKey(userPublicId));
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    private String buildKey(String userPublicId) {
        return KEY_PREFIX + userPublicId;
    }
}
