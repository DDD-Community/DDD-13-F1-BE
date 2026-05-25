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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
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

    @Test
    void poll_returns_oldest_generation_job_record() {
        MapRecord<String, Object, Object> record = MapRecord
                .create("quiz:generation:jobs", Map.<Object, Object>of(
                        "generationJobId", "900",
                        "quizSessionId", "500",
                        "quizSessionPublicId", "quiz-session-public-id",
                        "jobId", "quiz-job-public-id",
                        "userId", "1"
                ))
                .withId(RecordId.of("1748130000000-0"));
        when(streamOperations.range(eq("quiz:generation:jobs"), any(Range.class), any(Limit.class)))
                .thenReturn(List.of(record));

        Optional<QuizGenerationQueueRecord> polledRecord = redisQuizGenerationQueue.poll();

        assertThat(polledRecord).isPresent();
        assertThat(polledRecord.get().messageId()).isEqualTo("1748130000000-0");
        assertThat(polledRecord.get().message().generationJobId()).isEqualTo(900L);
        assertThat(polledRecord.get().message().quizSessionId()).isEqualTo(500L);
        assertThat(polledRecord.get().message().quizSessionPublicId()).isEqualTo("quiz-session-public-id");
        assertThat(polledRecord.get().message().jobId()).isEqualTo("quiz-job-public-id");
        assertThat(polledRecord.get().message().userId()).isEqualTo(1L);
    }

    @Test
    void acknowledge_deletes_generation_job_record() {
        redisQuizGenerationQueue.acknowledge("1748130000000-0");

        verify(streamOperations).delete("quiz:generation:jobs", "1748130000000-0");
    }
}
