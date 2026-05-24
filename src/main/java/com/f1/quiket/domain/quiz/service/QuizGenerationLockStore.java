package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 사용자당 퀴즈 생성 1건 제한용 분산락 저장소
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizGenerationLockStore {

    private static final String LOCK_KEY_PREFIX = "quiz:gen:lock:";
    private static final String LOCK_VALUE = "locked";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10L);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 사용자 단위 락 획득 (NX + TTL)
     */
    public void acquire(Long userId) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(buildKey(userId), LOCK_VALUE, LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new CustomException(ErrorCode.QUIZ_GENERATION_IN_PROGRESS);
            }
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 락 해제 (실패 시 로그 후 무시, TTL로 자연 만료)
     */
    public void release(Long userId) {
        try {
            stringRedisTemplate.delete(buildKey(userId));
        } catch (DataAccessException e) {
            log.warn("퀴즈 생성 분산락 해제 실패. userId={}", userId, e);
        }
    }

    private String buildKey(Long userId) {
        return LOCK_KEY_PREFIX + userId;
    }
}
