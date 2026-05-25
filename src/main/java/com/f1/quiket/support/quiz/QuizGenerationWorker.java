package com.f1.quiket.support.quiz;

import com.f1.quiket.domain.quiz.service.QuizGenerationJobProcessor;
import com.f1.quiket.domain.quiz.service.QuizGenerationQueue;
import com.f1.quiket.domain.quiz.service.QuizGenerationQueueRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis Stream에서 퀴즈 생성 작업을 poll하여 처리하는 단일 워커.
 *
 * <p>{@code @Scheduled} 기본 단일 스레드라 한 인스턴스 내에서 동시 실행 없음.
 * 다중 인스턴스 운영은 {@link com.f1.quiket.domain.quiz.service.RedisQuizGenerationQueue}의
 * 단일 워커 가정과 충돌하므로 Consumer Group 도입 전까지 금지.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "quiket.quiz.generation.worker.enabled", havingValue = "true")
public class QuizGenerationWorker {

    private final QuizGenerationQueue quizGenerationQueue;
    private final QuizGenerationJobProcessor quizGenerationJobProcessor;

    /**
     * 큐에서 1건 poll하여 처리하고 성공/실패 무관 ack한다.
     */
    @Scheduled(fixedDelayString = "${quiket.quiz.generation.worker.fixed-delay-ms:1000}")
    public void pollAndProcess() {
        quizGenerationQueue.poll()
                .ifPresent(this::processRecord);
    }

    /**
     * timeout_at 초과한 in_progress 작업을 timeout 상태로 정리한다.
     */
    @Scheduled(fixedDelayString = "${quiket.quiz.generation.worker.timeout-sweep-fixed-delay-ms:60000}")
    public void sweepTimeouts() {
        quizGenerationJobProcessor.markTimedOutJobs();
    }

    private void processRecord(QuizGenerationQueueRecord record) {
        boolean processed = quizGenerationJobProcessor.process(record.message());
        if (processed) {
            quizGenerationQueue.acknowledge(record.messageId());
        }
    }
}
