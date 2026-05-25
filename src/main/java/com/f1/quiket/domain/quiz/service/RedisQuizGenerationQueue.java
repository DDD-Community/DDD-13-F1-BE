package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisQuizGenerationQueue implements QuizGenerationQueue {

    private static final String STREAM_KEY = "quiz:generation:jobs";

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

    private Map<String, String> toRecord(QuizGenerationQueueMessage message) {
        return Map.of(
                "generationJobId", String.valueOf(message.generationJobId()),
                "quizSessionId", String.valueOf(message.quizSessionId()),
                "quizSessionPublicId", message.quizSessionPublicId(),
                "jobId", message.jobId(),
                "userId", String.valueOf(message.userId())
        );
    }
}
