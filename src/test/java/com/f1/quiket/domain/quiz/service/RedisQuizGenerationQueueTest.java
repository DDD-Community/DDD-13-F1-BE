package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisQuizGenerationQueueTest {

    private StringRedisTemplate stringRedisTemplate;
    private StreamOperations<String, Object, Object> streamOperations;
    private RedisQuizGenerationQueue redisQuizGenerationQueue;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        streamOperations = mock(StreamOperations.class);
        redisQuizGenerationQueue = new RedisQuizGenerationQueue(stringRedisTemplate);

        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    void enqueue_adds_generation_job_to_redis_stream() {
        QuizGenerationQueueMessage message = new QuizGenerationQueueMessage(
                900L,
                500L,
                "quiz-session-public-id",
                "quiz-job-public-id",
                1L
        );
        when(streamOperations.add(eq("quiz:generation:jobs"), any(Map.class)))
                .thenReturn(RecordId.of("1748130000000-0"));

        String messageId = redisQuizGenerationQueue.enqueue(message);

        assertThat(messageId).isEqualTo("1748130000000-0");

        ArgumentCaptor<Map<String, String>> recordCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("quiz:generation:jobs"), recordCaptor.capture());
        assertThat(recordCaptor.getValue())
                .containsEntry("generationJobId", "900")
                .containsEntry("quizSessionId", "500")
                .containsEntry("quizSessionPublicId", "quiz-session-public-id")
                .containsEntry("jobId", "quiz-job-public-id")
                .containsEntry("userId", "1");
    }

    @Test
    void enqueue_throws_service_unavailable_when_redis_fails() {
        QuizGenerationQueueMessage message = new QuizGenerationQueueMessage(
                900L,
                500L,
                "quiz-session-public-id",
                "quiz-job-public-id",
                1L
        );
        when(streamOperations.add(eq("quiz:generation:jobs"), any(Map.class)))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatThrownBy(() -> redisQuizGenerationQueue.enqueue(message))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }
}
