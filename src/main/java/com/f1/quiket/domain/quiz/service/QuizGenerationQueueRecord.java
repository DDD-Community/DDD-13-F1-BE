package com.f1.quiket.domain.quiz.service;

public record QuizGenerationQueueRecord(
        String messageId,
        QuizGenerationQueueMessage message
) {
}
