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
