package com.f1.quiket.domain.quiz.event;

import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.service.QuizGenerationQueueMessage;

/**
 * 퀴즈 생성 작업이 DB에 안전하게 적재된 후 Redis 큐 발행을 요청하는 이벤트.
 *
 * - 발행 시점: 퀴즈 세션 생성 트랜잭션 *안*
 * - 처리 시점: 트랜잭션 commit *이후* ({@code AFTER_COMMIT})
 * - 목적: DB rollback 시 Redis Stream에 orphan 메시지가 남지 않도록 정합성 보장
 */
public record QuizGenerationEnqueueRequestedEvent(QuizGenerationQueueMessage message) {

    public static QuizGenerationEnqueueRequestedEvent of(QuizGenerationJob generationJob, QuizSession quizSession) {
        return new QuizGenerationEnqueueRequestedEvent(QuizGenerationQueueMessage.of(generationJob, quizSession));
    }
}
