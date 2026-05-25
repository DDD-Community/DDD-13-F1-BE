package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import com.f1.quiket.domain.quiz.entity.QuizSession;

public record QuizGenerationQueueMessage(
        Long generationJobId,
        Long quizSessionId,
        String quizSessionPublicId,
        String jobId,
        Long userId
) {

    public static QuizGenerationQueueMessage of(QuizGenerationJob generationJob, QuizSession quizSession) {
        return new QuizGenerationQueueMessage(
                generationJob.getId(),
                quizSession.getId(),
                quizSession.getPublicId(),
                quizSession.getJobId(),
                quizSession.getUserId()
        );
    }
}
