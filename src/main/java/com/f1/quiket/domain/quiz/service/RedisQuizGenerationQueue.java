package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 기반 퀴즈 생성 큐 — MVP 단일 워커 가정.
 *
 * <h3>제약</h3>
 * <ul>
 *     <li>{@code XRANGE} + {@code XDEL} 패턴 — Consumer Group({@code XREADGROUP}/{@code XACK}) 미사용</li>
 *     <li>워커 인스턴스가 2개 이상이면 동일 메시지가 중복 처리될 수 있음 — 단일 워커 한정으로만 안전</li>
 *     <li>워커가 처리 도중 죽으면 {@link #acknowledge(String)}가 호출되지 않아 같은 메시지가 다음 poll에 재선택 → 처리 idempotency 가정 필요</li>
 * </ul>
 *
 * <p>운영 트래픽 증가 또는 워커 다중화 시 Consumer Group 기반 redelivery로 전환 필요 (후속 이슈).</p>
 */
@Component
@RequiredArgsConstructor
public class RedisQuizGenerationQueue implements QuizGenerationQueue {

    private static final String STREAM_KEY = "quiz:generation:jobs";
    private static final int POLL_COUNT = 1;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String enqueue(QuizGenerationQueueMessage message) {
        try {
            RecordId recordId = stringRedisTemplate.opsForStream()
                    .add(STREAM_KEY, toRecord(message));
            if (recordId == null) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            }
            return recordId.getValue();
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public Optional<QuizGenerationQueueRecord> poll() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .range(STREAM_KEY, Range.unbounded(), Limit.limit().count(POLL_COUNT));
            if (records == null || records.isEmpty()) {
                return Optional.empty();
            }

            MapRecord<String, Object, Object> record = records.get(0);
            return Optional.of(new QuizGenerationQueueRecord(
                    record.getId().getValue(),
                    toMessage(record.getValue())
            ));
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public void acknowledge(String messageId) {
        try {
            stringRedisTemplate.opsForStream().delete(STREAM_KEY, messageId);
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    private Map<String, String> toRecord(QuizGenerationQueueMessage message) {
        return Map.of(
                "generationJobId", String.valueOf(message.generationJobId()),
                "quizSessionId", String.valueOf(message.quizSessionId()),
                "quizSessionPublicId", message.quizSessionPublicId(),
                "jobId", message.jobId(),
                "userId", String.valueOf(message.userId())
        );
    }

    private QuizGenerationQueueMessage toMessage(Map<Object, Object> record) {
        return new QuizGenerationQueueMessage(
                parseLong(record, "generationJobId"),
                parseLong(record, "quizSessionId"),
                value(record, "quizSessionPublicId"),
                value(record, "jobId"),
                parseLong(record, "userId")
        );
    }

    private Long parseLong(Map<Object, Object> record, String key) {
        return Long.valueOf(value(record, key));
    }

    private String value(Map<Object, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return String.valueOf(value);
    }
}
