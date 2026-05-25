package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import com.f1.quiket.domain.quiz.event.QuizGenerationEnqueueRequestedEvent;
import com.f1.quiket.domain.quiz.repository.QuizGenerationJobRepository;
import com.f1.quiket.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 퀴즈 생성 큐 발행 이벤트 리스너.
 *
 * - 호출 측 트랜잭션 commit 이후 별도 트랜잭션(REQUIRES_NEW)에서 Redis 큐 발행
 * - enqueue 실패 시 generation_job을 failed 상태로 마킹해 사용자 status 조회로 노출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizGenerationEnqueueListener {

    private static final String FAIL_CODE_ENQUEUE = "enqueue_failed";

    private final QuizGenerationQueue quizGenerationQueue;
    private final QuizGenerationJobRepository quizGenerationJobRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(QuizGenerationEnqueueRequestedEvent event) {
        Long generationJobId = event.message().generationJobId();
        try {
            String messageId = quizGenerationQueue.enqueue(event.message());
            quizGenerationJobRepository.findById(generationJobId)
                    .ifPresent(job -> job.assignMqMessageId(messageId));
        } catch (CustomException e) {
            log.error("퀴즈 생성 큐 발행 실패 — generation_job을 failed로 마킹. generationJobId={}", generationJobId, e);
            quizGenerationJobRepository.findById(generationJobId)
                    .ifPresent(this::markEnqueueFailed);
        }
    }

    private void markEnqueueFailed(QuizGenerationJob job) {
        job.markFailed(FAIL_CODE_ENQUEUE, "퀴즈 생성 큐 발행에 실패했습니다.", true);
    }
}
