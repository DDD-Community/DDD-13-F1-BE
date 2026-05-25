package com.f1.quiket.support.quiz;

import com.f1.quiket.domain.quiz.service.QuizGenerationJobProcessor;
import com.f1.quiket.domain.quiz.service.QuizGenerationQueue;
import com.f1.quiket.domain.quiz.service.QuizGenerationQueueRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "quiket.quiz.generation.worker.enabled", havingValue = "true")
public class QuizGenerationWorker {

    private final QuizGenerationQueue quizGenerationQueue;
    private final QuizGenerationJobProcessor quizGenerationJobProcessor;

    @Scheduled(fixedDelayString = "${quiket.quiz.generation.worker.fixed-delay-ms:1000}")
    public void consume() {
        quizGenerationQueue.poll()
                .ifPresent(this::processRecord);
    }

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
